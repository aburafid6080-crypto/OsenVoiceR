"use client";
import { useMemo, useState } from "react";
type User = { user_id:string; premium:boolean; updated_at?:string };
type Site = { id:string; name:string; domain:string|null; status:string; created_at:string };
type AppProject = { id:string; name:string; package_name:string|null; platform:string; status:string; created_at:string };
type Log = { id:number; actor_email:string; action:string; details:Record<string,unknown>; created_at:string };
const rawUrl=process.env.NEXT_PUBLIC_SUPABASE_URL??"";
const supabaseUrl=(()=>{try{const u=new URL(rawUrl.trim());return u.protocol+"//"+u.host}catch{return rawUrl.trim().replace(/\/+$/,"")}})();
const anonKey=process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY??"";
const OWNER="Abu Rafid";
const appNav=[["overview","Overview","◈"],["app-projects","App projects","▣"],["premium","Premium","◇"],["users","Users","▣"],["app-control","App control","⚙"],["activity","Activity log","≋"],["integrations","Integrations","⌘"]];
const websiteNav=[["websites","Website projects","◎"]];
export default function Home(){
 const[email,setEmail]=useState("");const[password,setPassword]=useState("");const[token,setToken]=useState("");
 const[busy,setBusy]=useState(false);const[workspace,setWorkspace]=useState<"chooser"|"website"|"app">("chooser");const[mobileMenuOpen,setMobileMenuOpen]=useState(false);const[activeSection,setActiveSection]=useState("overview");const[notice,setNotice]=useState("");const[noticeError,setNoticeError]=useState(false);
 const[enabled,setEnabled]=useState(false);const[users,setUsers]=useState<User[]>([]);const[logs,setLogs]=useState<Log[]>([]);
 const[newId,setNewId]=useState("");const[newPass,setNewPass]=useState("");const[userSearch,setUserSearch]=useState("");
 const[maintenance,setMaintenance]=useState(false);const[forceUpdate,setForceUpdate]=useState(false);const[announcement,setAnnouncement]=useState("");
 const[sites,setSites]=useState<Site[]>([]);const[siteName,setSiteName]=useState("");const[siteSearch,setSiteSearch]=useState("");const[appProjects,setAppProjects]=useState<AppProject[]>([]);const[appName,setAppName]=useState("");const[appSearch,setAppSearch]=useState("");
 async function loadDashboard(t=token,target=workspace){
  const endpoint=target==="website"?"/api/website-admin":"/api/admin";
  const r=await fetch(endpoint,{headers:{Authorization:"Bearer "+t},cache:"no-store"});const d=await r.json();
  if(!r.ok)throw new Error(d.error||"Could not load dashboard");
  if(target==="website"){setSites(d.sites||[]);return;}
  setEnabled(Boolean(d.settings?.premium_enabled));setUsers(d.users||[]);setMaintenance(Boolean(d.config?.maintenance_mode));
  setForceUpdate(Boolean(d.config?.force_update));setAnnouncement(d.config?.announcement||"");setAppProjects(d.appProjects||[]);setLogs(d.logs||[]);
 }
 async function api(action:string,extra:Record<string,unknown>={}){const websiteActions=["create_site_project","set_site_status","delete_site_project"];const endpoint=websiteActions.includes(action)?"/api/website-admin":"/api/admin";const r=await fetch(endpoint,{method:"POST",headers:{"Content-Type":"application/json",Authorization:"Bearer "+token},body:JSON.stringify({action,...extra})});const d=await r.json();if(!r.ok)throw new Error(d.error||"Request failed");return d;}
 async function run(action:string,extra:Record<string,unknown>={},success="Changes saved."){setBusy(true);setNotice("");setNoticeError(false);try{await api(action,extra);await loadDashboard();setNotice(success)}catch(e){setNotice(e instanceof Error?e.message:"Action failed");setNoticeError(true)}finally{setBusy(false)}}
 async function login(e:React.FormEvent){e.preventDefault();setBusy(true);setNotice("");setNoticeError(false);try{if(!supabaseUrl||!anonKey)throw new Error("Admin website environment is not configured.");const r=await fetch(supabaseUrl+"/auth/v1/token?grant_type=password",{method:"POST",headers:{"Content-Type":"application/json",apikey:anonKey},body:JSON.stringify({email,password})});const d=await r.json();if(!r.ok)throw new Error(d.msg||d.message||"Login failed");await loadDashboard(d.access_token,workspace);setToken(d.access_token);setActiveSection("overview");setPassword("")}catch(e){setNotice(e instanceof Error?e.message:"Could not sign in");setNoticeError(true)}finally{setBusy(false)}}
 function logout(){setToken("");setUsers([]);setSites([]);setAppProjects([]);setLogs([]);setEnabled(false);setNotice("")}
 const filteredUsers=useMemo(()=>users.filter(u=>u.user_id.toLowerCase().includes(userSearch.toLowerCase())),[users,userSearch]);
 const filteredSites=useMemo(()=>sites.filter(s=>s.name.toLowerCase().includes(siteSearch.toLowerCase())),[sites,siteSearch]);
 const filteredAppProjects=useMemo(()=>appProjects.filter(p=>p.name.toLowerCase().includes(appSearch.toLowerCase())),[appProjects,appSearch]);
 const premiumCount=users.filter(u=>u.premium).length;
 const currentNav=workspace==="website"?websiteNav:appNav;
 return <main className="nexus-shell">
  {token&&<header className="nexus-topbar"><a className="nexus-brand" href="#overview"><span className="brand-mark">A</span><span><b>OSEN <i>AUREX</i></b><small>ADMIN PANEL · PRIVATE OWNER</small></span></a><div className="topbar-right"><span className="secure-state"><i/> SECURE ADMIN</span>{token&&<div className="owner-chip"><span className="owner-avatar">AR</span><span><b>{OWNER}</b><small>OWNER</small></span><button className="logout-btn" onClick={logout}>LOG OUT</button></div>}</div></header>}
  {!token?<section className="nexus-login-page">
   <div className="login-ambient login-ambient-one"/><div className="login-ambient login-ambient-two"/>
   <div className="login-stage">
    <div className="login-visual">
     <div className="login-visual-grid"/>
     <div className="login-orbit login-orbit-a"/><div className="login-orbit login-orbit-b"/>
     <div className="login-character" aria-hidden="true">
      <svg viewBox="0 0 520 620" role="img" aria-label="Stylized silver-haired gaming guardian">
       <defs>
        <linearGradient id="nxHair" x1="0" y1="0" x2="1" y2="1"><stop stopColor="#fff9df"/><stop offset=".42" stopColor="#aeb9c8"/><stop offset="1" stopColor="#4a5261"/></linearGradient>
        <linearGradient id="nxArmor" x1="0" y1="0" x2="1" y2="1"><stop stopColor="#fff0b0"/><stop offset=".35" stopColor="#9b7530"/><stop offset="1" stopColor="#171923"/></linearGradient>
        <linearGradient id="nxFace" x1="0" y1="0" x2="1" y2="1"><stop stopColor="#e8d6c4"/><stop offset="1" stopColor="#776b6b"/></linearGradient>
        <filter id="nxGlow"><feGaussianBlur stdDeviation="7"/></filter>
       </defs>
       <ellipse cx="260" cy="265" rx="188" ry="188" fill="none" stroke="#e8bd58" strokeWidth="2" opacity=".65"/>
       <ellipse cx="260" cy="265" rx="205" ry="205" fill="none" stroke="#e8bd58" strokeWidth="1" strokeDasharray="3 13" opacity=".65"/>
       <path d="M260 26l17 31 35-10-6 37 32 18-34 17-1 38-43-20-43 20-1-38-34-17 32-18-6-37 35 10z" fill="#dcb65d" opacity=".95"/>
       <path d="M260 48l10 22 24-5-8 23 19 15-25 6-7 24-19-17-19 17-7-24-25-6 19-15-8-23 24 5z" fill="#fff0b5"/>
       <path d="M98 620q10-155 93-204l69-30 69 30q83 49 93 204z" fill="url(#nxArmor)" stroke="#d6ae56" strokeWidth="3"/>
       <path d="M165 421l95-55 95 55-44 95h-102z" fill="#111723" stroke="#d6ae56" strokeWidth="3"/>
       <path d="M177 195q-4-106 83-133 89 23 86 137l-23 112-62 58-67-58z" fill="url(#nxHair)" stroke="#f0d58c" strokeWidth="2"/>
       <path d="M174 197q-30-90 28-141l-4 92 38-87 5 94 49-100-7 111 62-72q28 81-4 144l-32-40-22 73-37-64-38 63-10-82-34 45z" fill="url(#nxHair)" stroke="#d9dce3" strokeWidth="2"/>
       <path d="M213 206q46-41 93 0l-9 100-38 42-38-42z" fill="url(#nxFace)"/>
       <path d="M211 239l49 13 49-13-12 46-37 17-37-17z" fill="#171923" stroke="#d7b45f" strokeWidth="3"/>
       <path d="M222 246l35 9-28 8zM298 246l-35 9 28 8z" fill="#e6c36e"/>
       <path d="M224 299l36 14 36-14-12 30h-48z" fill="#22232a" stroke="#a9843d" strokeWidth="2"/>
       <path d="M180 390l80-26 80 26-42 41h-76z" fill="#202631" stroke="#d7b45f" strokeWidth="3"/>
       <path d="M106 620l35-147 80-48 39 68 39-68 80 48 35 147" fill="none" stroke="#f0d58c" strokeWidth="5" opacity=".8"/>
       <path d="M260 366v116m-42-93 42 40 42-40" fill="none" stroke="#f6d47d" strokeWidth="4" opacity=".9"/>
       <circle cx="260" cy="266" r="192" fill="#e5b64b" opacity=".08" filter="url(#nxGlow)"/>
      </svg>
     </div>
     <div className="login-visual-caption"><span className="aurex-mark">A</span><b>OSEN <i>AUREX</i></b><small>THE ULTIMATE CONTROL CENTER</small></div>
     <div className="login-art-tag">PRIVATE OWNER ACCESS <span>✦</span></div>
    </div>
    <div className="login-panel-wrap">
     <div className="login-panel">
      <div className="login-panel-brand"><span className="aurex-mark">A</span><div><b>OSEN <em>AUREX</em></b><small>ADMINISTRATION PORTAL</small></div></div>
      <div className="login-panel-kicker"><i/> WELCOME BACK · OWNER</div>
      <h1>Owner <em>Login.</em></h1>
      <p>Sign in to access your private workspace and manage your projects.</p>
      <form onSubmit={login}>
       <label htmlFor="admin-email">OWNER EMAIL</label>
       <div className="login-field"><span>✉</span><input id="admin-email" type="email" autoComplete="username" value={email} onChange={e=>setEmail(e.target.value)} placeholder="Enter your email address" required/></div>
       <label htmlFor="admin-password">PASSWORD</label>
       <div className="login-field"><span>♙</span><input id="admin-password" type="password" autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)} placeholder="Enter your password" required/></div>
       <button className="login-submit" disabled={busy}>{busy?<><span className="login-spinner"/> VERIFYING ACCESS…</>:"SECURE LOGIN　↗"}</button>
      </form>
      <div className="login-security"><span>♢</span> ENCRYPTED OWNER AUTHENTICATION <i/> SECURE ACCESS</div>
      {notice&&<div className={noticeError?"alert error":"alert"} role="status">{notice}</div>}
     </div>
     <div className="login-benefits"><span><b>✦</b> ADVANCED CONTROL</span><span><b>♢</b> ULTIMATE SECURITY</span><span><b>◇</b> PREMIUM EXPERIENCE</span></div>
    </div>
   </div>
   <footer className="login-page-footer"><span>Ⓐ OSEN AUREX</span><span>OWNER ACCESS ONLY · © 2026</span></footer>
  </section>:workspace==="chooser"?<section className="workspace-picker">
   <div className="picker-heading"><span className="section-kicker">ADMIN PANEL · PRIVATE OWNER</span><h1>Choose your <em>workspace</em></h1><p>Website projects and Android app controls stay in separate work areas.</p></div>
   <div className="picker-grid">
    <button className="workspace-tile website-tile" onClick={()=>{setWorkspace("website");setActiveSection("websites");loadDashboard(token,"website").catch(e=>{setNotice(e instanceof Error?e.message:"Could not load Website Studio");setNoticeError(true)})}}><span className="tile-icon">◎</span><span className="tile-kicker">01 / WEB PLATFORM</span><strong>WEBSITE CENTER</strong><small>Build and manage websites, projects, domains and hosting.</small><span className="tile-cta">OPEN WEBSITE CENTER ↗</span></button>
    <button className="workspace-tile app-tile" onClick={()=>{setWorkspace("app");setActiveSection("overview");loadDashboard(token,"app").catch(e=>{setNotice(e instanceof Error?e.message:"Could not load App Studio");setNoticeError(true)})}}><span className="tile-icon">▣</span><span className="tile-kicker">02 / ANDROID PLATFORM</span><strong>ANDROID APP CENTER</strong><small>Manage app access, Premium, Voice and Game Booster controls.</small><span className="tile-cta">OPEN APP CENTER ↗</span></button>
   </div>
   <div className="picker-foot"><span>Ⓐ OSEN AUREX</span><span>ONE OWNER · TWO WORKSPACES</span><button onClick={logout}>LOG OUT</button></div>
  </section>:<div className="nexus-workspace">
   <aside className="side-nav"><button className="workspace-switch" onClick={()=>setWorkspace("chooser")}>← ALL WORKSPACES</button><div className="nav-caption">{workspace==="website"?"WEBSITE CENTER":"ANDROID APP CENTER"}</div>{currentNav.map(([id,label,ico])=><a key={id} href={"#"+id} aria-current={activeSection===id?"page":undefined} className={activeSection===id?"nav-active":""} onClick={()=>setActiveSection(id)}><span>{ico}</span>{label}</a>)}<div className="sidebar-bottom"><i/> SYSTEM READY<small>Owner-only workspace</small></div></aside>
   <div className="dashboard-content" data-active={activeSection} data-workspace={workspace}>
    <section id="overview" className="welcome-panel"><div className="workspace-context">{workspace==="website"?"WEBSITE CENTER":"ANDROID APP CENTER"}</div><div><div className="section-kicker">CONTROL CENTER　/　{(currentNav.find(([id])=>id===activeSection)?.[1]||"OVERVIEW").toUpperCase()}</div><h1>{activeSection==="overview"?"Welcome back,":activeSection==="premium"?"Premium":"Manage"} <em>{activeSection==="overview"?OWNER:activeSection==="premium"?"Access":" "+(currentNav.find(([id])=>id===activeSection)?.[1]||"Workspace")}.</em></h1><p>{activeSection==="premium"?"Manage global Premium availability and securely update the app verification password.":activeSection==="overview"?"Your private workspace to manage OSEN apps, access and website projects.":`Manage ${currentNav.find(([id])=>id===activeSection)?.[1]||"workspace"} settings from your private OSEN AUREX console.`}</p></div><div className="welcome-status"><i/> PRIVATE WORKSPACE</div></section>
    <div className="metric-grid">
     <article className="metric-card metric-gold"><span>PREMIUM SYSTEM</span><strong className={enabled?"status-on":"status-off"}>{enabled?"ENABLED":"DISABLED"}</strong><small>Global Premium access</small><button className="primary-btn" disabled={busy} onClick={()=>run("set_premium_enabled",{enabled:!enabled},enabled?"Premium access disabled.":"Premium access enabled.")}>{enabled?"TURN OFF PREMIUM":"TURN ON PREMIUM"}</button></article>
     <article className="metric-card"><span>PREMIUM ACCESS RECORDS</span><strong>{users.length}</strong><small>{premiumCount} currently approved · {users.length-premiumCount} not approved</small><a className="text-link" href="#users" onClick={()=>setActiveSection("users")}>Manage users　↗</a></article>

     <article className="metric-card"><span>MAINTENANCE MODE</span><strong className={maintenance?"status-off":"status-on"}>{maintenance?"ON":"OFF"}</strong><small>Remote setting · app integration pending</small><a className="text-link" href="#app-control" onClick={()=>setActiveSection("app-control")}>App settings　↗</a></article>
    </div>
    <div className="content-grid">
     <section id="premium" className="panel-card"><div className="panel-heading"><div><span className="section-kicker">SECURITY CONTROL</span><h2>Premium <em>password</em></h2></div><span className="panel-icon">◇</span></div><p>Change the password used by the app's Premium verification. Only a secure hash is stored on the server.</p><div className="inline-form"><input type="password" autoComplete="new-password" minLength={8} maxLength={128} value={newPass} onChange={e=>setNewPass(e.target.value)} placeholder="New password · 8–128 characters"/><button disabled={busy||newPass.length<8} onClick={async()=>{await run("change_premium_password",{password:newPass},"Premium password updated.");setNewPass("")}}>UPDATE PASSWORD</button></div></section>
     <section id="app-control" className="panel-card"><div className="panel-heading"><div><span className="section-kicker">GLOBAL APP CONTROL</span><h2>Remote <em>configuration</em></h2></div><span className="panel-icon">⚙</span></div><p>These values are saved centrally. The Android app must fetch this configuration before it can act on it.</p><div className="setting-row"><div><b>Maintenance mode</b><small>Show maintenance state in the app after integration</small></div><strong className={maintenance?"status-off":"status-on"}>{maintenance?"ON":"OFF"}</strong><button className="outline-btn" disabled={busy} onClick={()=>run("set_app_config",{maintenance_mode:!maintenance})}>{maintenance?"TURN OFF":"TURN ON"}</button></div><div className="setting-row"><div><b>Force update notice</b><small>Update warning after Android integration</small></div><strong className={forceUpdate?"status-on":"status-off"}>{forceUpdate?"ON":"OFF"}</strong><button className="outline-btn" disabled={busy} onClick={()=>run("set_app_config",{force_update:!forceUpdate})}>{forceUpdate?"TURN OFF":"TURN ON"}</button></div><div className="inline-form"><input maxLength={500} value={announcement} onChange={e=>setAnnouncement(e.target.value)} placeholder="Announcement for app users · max 500 characters"/><button disabled={busy} onClick={()=>run("set_app_config",{announcement},"Announcement saved.")}>SAVE NOTICE</button></div></section>
     <section id="users" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">ACCESS MANAGEMENT</span><h2>Premium <em>users</em></h2></div><span className="count-pill">{users.length} RECORDS</span></div><p>Grant or revoke Premium access using the exact app user ID. This list is not yet a complete list of every installed device.</p><div className="inline-form"><input value={newId} onChange={e=>setNewId(e.target.value)} placeholder="App user ID"/><button disabled={busy||!newId.trim()} onClick={()=>run("set_user_premium",{user_id:newId.trim(),premium:true},"Premium access granted.")}>GRANT ACCESS</button></div><div className="table-tools"><span>USER ACCESS LIST</span><input className="search-input" value={userSearch} onChange={e=>setUserSearch(e.target.value)} placeholder="Search user ID"/></div><div className="data-list">{filteredUsers.length?filteredUsers.map(u=><div className="data-row" key={u.user_id}><code>{u.user_id}</code><span className={u.premium?"status-on":"status-off"}>{u.premium?"PREMIUM":"FREE"}</span><small>{u.updated_at?new Date(u.updated_at).toLocaleDateString():"—"}</small><button className="outline-btn" disabled={busy} onClick={()=>run("set_user_premium",{user_id:u.user_id,premium:!u.premium},u.premium?"Access revoked.":"Access granted.")}>{u.premium?"REVOKE":"GRANT"}</button><button className="danger-btn" disabled={busy} onClick={()=>{if(window.confirm("Remove this user access record?"))run("delete_premium_user",{user_id:u.user_id},"User record removed.")}}>REMOVE</button></div>):<div className="empty-state">No matching user records.</div>}</div></section>
     {workspace==="app"&&<section id="app-projects" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">ANDROID APP PROJECTS</span><h2>App <em>projects</em></h2></div><span className="panel-icon">▣</span></div><p>Create and track Android app project records here. APK/AAB compilation and store publishing need a connected build service and signing configuration.</p><div className="inline-form"><input maxLength={80} value={appName} onChange={e=>setAppName(e.target.value)} placeholder="Android app project name"/><button disabled={busy||!appName.trim()} onClick={async()=>{await run("create_app_project",{name:appName.trim()},"Android app project created.");setAppName("")}}>＋ CREATE APP PROJECT</button></div><div className="table-tools"><span>YOUR ANDROID PROJECTS</span><input className="search-input" value={appSearch} onChange={e=>setAppSearch(e.target.value)} placeholder="Search app projects"/></div><div className="data-list">{filteredAppProjects.length?filteredAppProjects.map(p=><div className="data-row site-row" key={p.id}><b>{p.name}</b><code>{p.package_name||"Package name not set"}</code><span className="draft-pill">{p.status.toUpperCase()}</span><small>{new Date(p.created_at).toLocaleDateString()}</small></div>):<div className="empty-state">No app projects yet. Create your first Android project above.</div>}</div><div className="integration-note"><b>ANDROID BUILD STATUS</b><span>APK / AAB build service · Not connected</span><span>Signing key · Not configured</span><span>Store publishing · Not connected</span></div></section>}
     <section id="websites" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">WEBSITE & HOSTING MANAGER</span><h2>Website <em>projects</em></h2></div><span className="panel-icon">◎</span></div><p>Your private website workspace. Create project drafts here for your own sites or client sites. Only your owner account can manage these projects. Publishing and custom domains require a connected hosting provider account.</p><div className="inline-form"><input maxLength={80} value={siteName} onChange={e=>setSiteName(e.target.value)} placeholder="Website / client project name"/><button disabled={busy||!siteName.trim()} onClick={async()=>{await run("create_site_project",{name:siteName.trim()},"Website project created.");setSiteName("")}}>＋ CREATE PROJECT</button></div><div className="table-tools"><span>YOUR PROJECTS</span><input className="search-input" value={siteSearch} onChange={e=>setSiteSearch(e.target.value)} placeholder="Search projects"/></div><div className="data-list">{filteredSites.length?filteredSites.map(s=><div className="data-row site-row" key={s.id}><b>{s.name}</b><code>{s.domain||"Domain not connected"}</code><span className="draft-pill">{s.status.toUpperCase()}</span><button className="outline-btn" disabled={busy||!["draft","paused"].includes(s.status)} onClick={()=>run("set_site_status",{id:s.id,status:s.status==="paused"?"draft":"paused"},s.status==="paused"?"Project resumed.":"Project paused.")}>{s.status==="paused"?"RESUME":"PAUSE"}</button><button className="danger-btn" disabled={busy} onClick={()=>{if(window.confirm("Delete this project record?"))run("delete_site_project",{id:s.id},"Project record deleted.")}}>DELETE</button></div>):<div className="empty-state">No projects yet. Create your first project above.</div>}</div><div className="integration-note"><b>HOSTING STATUS</b><span>Not connected</span><span>DOMAIN · Not connected</span><span>DEPLOYMENT · Not available yet</span></div></section>
     <section id="activity" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">SECURITY & TRANSPARENCY</span><h2>Admin <em>activity log</em></h2></div><span className="count-pill">LATEST 25</span></div><p>Recent administrative actions recorded by the server.</p><div className="data-list">{logs.length?logs.map(log=><div className="log-row" key={log.id}><span className="log-dot"/><div><b>{log.action.replaceAll("_"," ").toUpperCase()}</b><small>{log.actor_email} · {new Date(log.created_at).toLocaleString()}</small></div></div>):<div className="empty-state">No activity recorded yet.</div>}</div></section>
     <section id="integrations" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">PLATFORM INTEGRATIONS</span><h2>Owner <em>tools</em></h2></div><span className="panel-icon">⌘</span></div><div className="integration-list"><div><b>Android device registry</b><span>Pending · app must register a generated device UUID</span></div><div><b>Voice & Booster remote controls</b><span>Pending · Android app must fetch and apply remote settings</span></div><div><b>Vercel hosting & custom domains</b><span>Pending · provider API credentials and project linking required</span></div><div><b>Audit trail</b><span className="ready-label">ACTIVE · admin actions are logged</span></div></div></section>
    </div>
    {notice&&<div className={noticeError?"alert error":"alert"} role="status">{notice}</div>}
    <footer className="nexus-footer"><span>OSEN AUREX　·　OWNER CONSOLE</span><span>BLACK / SKY BLUE / GOLD　·　{OWNER}</span></footer>
   </div>
  </div>}
 </main>;
}
