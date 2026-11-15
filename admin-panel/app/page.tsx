"use client";
import { useEffect, useMemo, useState } from "react";
type User = { user_id:string; premium:boolean; updated_at?:string };
type Site = { id:string; name:string; domain:string|null; status:string; created_at:string; template?:string; content?:{headline?:string;description?:string;button_text?:string;accent?:string} };
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
 const[busy,setBusy]=useState(false);const[workspace,setWorkspace]=useState<"home"|"chooser"|"website"|"app">("home");const[mobileMenuOpen,setMobileMenuOpen]=useState(false);const[activeSection,setActiveSection]=useState("overview");const[notice,setNotice]=useState("");const[noticeError,setNoticeError]=useState(false);
 const[enabled,setEnabled]=useState(false);const[passwordChangedAt,setPasswordChangedAt]=useState<string|null>(null);const[showNewPass,setShowNewPass]=useState(false);const[newPassConfirm,setNewPassConfirm]=useState("");const[users,setUsers]=useState<User[]>([]);const[logs,setLogs]=useState<Log[]>([]);
 const[newId,setNewId]=useState("");const[newPass,setNewPass]=useState("");const[userSearch,setUserSearch]=useState("");
 const[maintenance,setMaintenance]=useState(false);const[forceUpdate,setForceUpdate]=useState(false);const[announcement,setAnnouncement]=useState("");
 const[sites,setSites]=useState<Site[]>([]);const[siteName,setSiteName]=useState("");const[siteSearch,setSiteSearch]=useState("");const[activeSiteId,setActiveSiteId]=useState("");const[siteTemplate,setSiteTemplate]=useState("portfolio");const[siteHeadline,setSiteHeadline]=useState("Build something remarkable.");const[siteDescription,setSiteDescription]=useState("Introduce your work, your ideas and what makes your project unique.");const[siteButtonText,setSiteButtonText]=useState("Get started");const[siteAccent,setSiteAccent]=useState("#d8b15e");const[previewOpen,setPreviewOpen]=useState(false);const[sitePreviewDevice,setSitePreviewDevice]=useState<"desktop"|"mobile">("desktop");const[appProjects,setAppProjects]=useState<AppProject[]>([]);const[appName,setAppName]=useState("");const[appSearch,setAppSearch]=useState("");
 async function readApiResponse(r:Response,fallback:string){
  let d:any={};
  try{d=await r.json()}catch{d={error:fallback}}
  if(r.status===401){
   if(token)logout();
   throw new Error("Your admin session has expired. Please sign in again.");
  }
  if(!r.ok)throw new Error(d.error||fallback);
  return d;
 }
 async function loadDashboard(t=token,target=workspace){
  const endpoint=target==="website"?"/api/website-admin":"/api/admin";
  const r=await fetch(endpoint,{headers:{Authorization:"Bearer "+t},cache:"no-store"});
  const d=await readApiResponse(r,"Could not load dashboard");
  if(target==="website"){setSites(d.sites||[]);return;}
  setEnabled(Boolean(d.settings?.premium_enabled));setPasswordChangedAt(d.settings?.premium_password_changed_at||null);setUsers(d.users||[]);setMaintenance(Boolean(d.config?.maintenance_mode));
  setForceUpdate(Boolean(d.config?.force_update));setAnnouncement(d.config?.announcement||"");setAppProjects(d.appProjects||[]);setLogs(d.logs||[]);
 }
 async function api(action:string,extra:Record<string,unknown>={}){const websiteActions=["create_site_project","set_site_status","delete_site_project","save_site_content"];const endpoint=websiteActions.includes(action)?"/api/website-admin":"/api/admin";const r=await fetch(endpoint,{method:"POST",headers:{"Content-Type":"application/json",Authorization:"Bearer "+token},body:JSON.stringify({action,...extra})});return readApiResponse(r,"Request failed");}
 async function run(action:string,extra:Record<string,unknown>={},success="Changes saved."){setBusy(true);setNotice("");setNoticeError(false);try{await api(action,extra);await loadDashboard();setNotice(success);return true}catch(e){setNotice(e instanceof Error?e.message:"Action failed");setNoticeError(true);return false}finally{setBusy(false)}}
 async function login(e:React.FormEvent){e.preventDefault();setBusy(true);setNotice("");setNoticeError(false);try{if(!supabaseUrl||!anonKey)throw new Error("Admin website environment is not configured.");const r=await fetch(supabaseUrl+"/auth/v1/token?grant_type=password",{method:"POST",headers:{"Content-Type":"application/json",apikey:anonKey},body:JSON.stringify({email,password})});const d=await r.json();if(!r.ok)throw new Error(d.msg||d.message||"Login failed");await loadDashboard(d.access_token,"app");await loadDashboard(d.access_token,"website");setToken(d.access_token);setWorkspace("home");window.history.replaceState({osenAdmin:true,workspace:"home",section:"overview"},"",window.location.pathname+"?workspace=home&section=overview");setActiveSection("overview");setPassword("")}catch(e){setNotice(e instanceof Error?e.message:"Could not sign in");setNoticeError(true)}finally{setBusy(false)}}
 function logout(){setToken("");setUsers([]);setSites([]);setAppProjects([]);setLogs([]);setEnabled(false);setNotice("")}
 const filteredUsers=useMemo(()=>users.filter(u=>u.user_id.toLowerCase().includes(userSearch.toLowerCase())),[users,userSearch]);
 const filteredSites=useMemo(()=>sites.filter(s=>s.name.toLowerCase().includes(siteSearch.toLowerCase())),[sites,siteSearch]);
 const filteredAppProjects=useMemo(()=>appProjects.filter(p=>p.name.toLowerCase().includes(appSearch.toLowerCase())),[appProjects,appSearch]);
 useEffect(()=>{
  const validSections=["overview","app-projects","premium","users","app-control","activity","integrations","websites"];
  function syncLocation(event?:PopStateEvent){
   const params=new URLSearchParams(window.location.search);
   const urlWorkspace=params.get("workspace");
   const urlSection=params.get("section");
   const state=(event?.state??window.history.state) as {osenAdmin?:boolean;workspace?:"home"|"chooser"|"website"|"app";section?:string}|null;
   const nextWorkspace=(urlWorkspace==="website"||urlWorkspace==="app"||urlWorkspace==="chooser"||urlWorkspace==="home")?urlWorkspace:state?.workspace;
   if(nextWorkspace==="home"||nextWorkspace==="chooser"||nextWorkspace==="website"||nextWorkspace==="app")setWorkspace(nextWorkspace);
   const section=urlSection||state?.section||"";
   if(validSections.includes(section))setActiveSection(section);
   else setActiveSection(nextWorkspace==="website"?"websites":"overview");
   setMobileMenuOpen(false);
  }
  function handlePop(event:PopStateEvent){syncLocation(event)}
  window.addEventListener("popstate",handlePop);
  syncLocation();
  return()=>window.removeEventListener("popstate",handlePop);
 },[]);
 const premiumCount=users.filter(u=>u.premium).length;
 const currentNav=workspace==="website"?websiteNav:appNav;
 function writeAdminUrl(nextWorkspace:"home"|"chooser"|"website"|"app",section:string){const url=new URL(window.location.href);url.searchParams.set("workspace",nextWorkspace);url.searchParams.set("section",section);window.history.pushState({osenAdmin:true,workspace:nextWorkspace,section},"",url.pathname+url.search)}
 function goHome(){writeAdminUrl("home","overview");setWorkspace("home");setActiveSection("overview");setMobileMenuOpen(false)}
 function navigateSection(section:string){const nextWorkspace=workspace==="website"?"website":"app";writeAdminUrl(nextWorkspace,section);setActiveSection(section);setMobileMenuOpen(false)}
 function openWorkspace(target:"website"|"app",sectionOverride?:string){const section=sectionOverride||(target==="website"?"websites":"overview");writeAdminUrl(target,section);setWorkspace(target);setActiveSection(section);setMobileMenuOpen(false);loadDashboard(token,target).catch(e=>{setNotice(e instanceof Error?e.message:"Could not load workspace");setNoticeError(true)})}
 return <main className="nexus-shell">
  {token&&<header className="nexus-topbar"><a className="nexus-brand" href="#overview" onClick={e=>{e.preventDefault();goHome()}}><span className="brand-mark">R</span><span><b>OSEN <i>AUREX</i></b><small>ADMIN PANEL · PRIVATE OWNER</small></span></a><div className="topbar-right"><span className="secure-state"><i/> SECURE ADMIN</span>{token&&<div className="owner-chip"><span className="owner-avatar">AR</span><span><b>{OWNER}</b><small>OWNER</small></span><button className="logout-btn" onClick={logout}>LOG OUT</button></div>}</div></header>}
  {token&&workspace==="home"?<section className="aurex-dashboard">
   <aside className="aurex-sidebar">
    <div className="aurex-side-brand"><span>R</span><b>OSEN AUREX<small>ADMIN PANEL</small></b></div>
    <button className="aurex-side-link active" onClick={goHome}>⌂　Dashboard</button>
    <button className="aurex-side-link" onClick={()=>openWorkspace("website")}>◎　Website Center</button>
    <button className="aurex-side-link" onClick={()=>openWorkspace("app")}>▣　Android App Center</button>
    <button className="aurex-side-link" onClick={()=>openWorkspace("app","users")}>♙　Users Management</button>
    <button className="aurex-side-link" onClick={()=>openWorkspace("app","premium")}>♛　Premium Control</button>
    <button className="aurex-side-link" onClick={()=>openWorkspace("app","activity")}>▥　Analytics / Activity</button>
    <button className="aurex-side-link" onClick={()=>openWorkspace("app","app-control")}>⚙　Settings</button>
    <button className="aurex-side-link" onClick={()=>openWorkspace("app","integrations")}>⬡　Security & Integrations</button>
    <button className="aurex-logout" onClick={logout}>⇥　LOG OUT</button>
    <div className="aurex-side-tag">OSEN AUREX<small>Powering Your Digital World</small></div>
   </aside>
   <div className="aurex-main">
    <header className="aurex-welcome"><div><span className="aurex-crown">♛</span><div><b>Welcome Back, <em>{OWNER}</em></b><small>Manage your workspace with power and ease.</small></div></div><span className="aurex-online">● Online</span><span className="aurex-profile">AR　 ADMIN</span></header>
    <div className="aurex-stats">
     <article><span>♟</span><b>{users.length}</b><strong>Total Users</strong><small>Live records</small></article>
     <article><span>◎</span><b>{sites.length}</b><strong>Website Projects</strong><small>Saved projects</small></article>
     <article><span>▣</span><b>{appProjects.length}</b><strong>App Projects</strong><small>Saved projects</small></article>
     <article><span>♛</span><b>{premiumCount}</b><strong>Premium Users</strong><small>{enabled?"Premium enabled":"Premium disabled"}</small></article>
    </div>
    <div className="aurex-dash-grid">
     <section className="aurex-panel aurex-activity"><header><b>▥　Latest Activity</b><button onClick={()=>openWorkspace("app","activity")}>VIEW ALL　→</button></header>
      {logs.length?logs.slice(0,5).map(log=><div className="aurex-activity-row" key={log.id}><i/> <span>{log.action.replaceAll("_"," ")}</span><small>{new Date(log.created_at).toLocaleString()}</small></div>):<p className="aurex-empty">No admin activity recorded yet.</p>}
     </section>
     <section className="aurex-panel aurex-quick"><header><b>ϟ　Quick Actions</b></header>
      <div className="aurex-action-grid">
       <button onClick={()=>openWorkspace("website")}><span>◎</span>Manage<br/>Website　›</button>
       <button onClick={()=>openWorkspace("app")}><span>▣</span>Manage<br/>Android App　›</button>
       <button onClick={()=>openWorkspace("app","premium")}><span>♛</span>Premium<br/>Control　›</button>
       <button onClick={()=>openWorkspace("app","app-control")}><span>⚙</span>Settings　›</button>
      </div>
     </section>
     <section className="aurex-panel aurex-status"><header><b>⬡　System Status</b><span>● LIVE DATA</span></header>
      <div className="aurex-status-grid"><div><b>{enabled?"ON":"OFF"}</b><small>Premium System</small></div><div><b>{maintenance?"ON":"OFF"}</b><small>Maintenance Mode</small></div><div><b>{sites.length}</b><small>Website Projects</small></div><div><b>{appProjects.length}</b><small>App Projects</small></div></div>
      <p>Hosting, analytics tracking and Android build services are not connected unless configured in Integrations.</p>
     </section>
    </div>
    {notice&&<div className={noticeError?"alert error":"alert"} role="status">{notice}</div>}
    <footer className="aurex-footer">© 2026 <b>OSEN AUREX.</b> All Rights Reserved. <em>Dream Big · Play Better</em><span>♛ Powered by Passion</span></footer>
   </div>
  </section>:null}
  {!token?<section className="nexus-login-page">
   <div className="login-ambient login-ambient-one"/><div className="login-ambient login-ambient-two"/>
   <div className="login-stage">
    <div className="login-visual">
     <div className="login-visual-grid"/>
     <div className="login-back-portrait" aria-hidden="true"><img src="/aurex-character-hat.webp" alt="" /></div>
     <div className="login-left-brand"><span className="login-crown">♛</span><strong>OSEN<br/><em>AUREX</em></strong><small>PLAY SMART · PLAY BETTER</small></div>
     <div className="login-orbit login-orbit-a"/><div className="login-orbit login-orbit-b"/>
     <div className="login-character" aria-hidden="true"><img src="/aurex-character-hat.webp" alt="" loading="eager" fetchPriority="high" decoding="async" /></div>
     <div className="login-visual-caption"><span className="aurex-mark">R</span><b>OSEN <i>AUREX</i></b><small>THE ULTIMATE CONTROL CENTER</small></div>
     <div className="login-art-tag">PRIVATE OWNER ACCESS <span>✦</span></div>
    </div>
    <div className="login-panel-wrap">
     <div className="login-panel">
      <div className="login-panel-brand"><span className="aurex-mark">R</span><div><b>OSEN <em>AUREX</em></b><small>ADMINISTRATION PORTAL</small></div></div>
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
  <footer className="login-page-footer"><span>Ⓡ OSEN AUREX</span><span>OWNER ACCESS ONLY · © 2026</span></footer>
  </section>:workspace==="home"?null:workspace==="chooser"?<section className="workspace-picker">
   <div className="picker-heading"><span className="section-kicker">ADMIN PANEL · PRIVATE OWNER</span><h1>Choose your <em>workspace</em></h1><p>Website projects and Android app controls stay in separate work areas.</p></div>
   <div className="picker-grid">
    <button className="workspace-tile website-tile" onClick={()=>{openWorkspace("website")}}><span className="tile-icon">◎</span><span className="tile-kicker">01 / WEB PLATFORM</span><strong>WEBSITE CENTER</strong><small>Build and manage websites, projects, domains and hosting.</small><span className="tile-cta">OPEN WEBSITE CENTER ↗</span></button>
    <button className="workspace-tile app-tile" onClick={()=>{openWorkspace("app")}}><span className="tile-icon">▣</span><span className="tile-kicker">02 / ANDROID PLATFORM</span><strong>ANDROID APP CENTER</strong><small>Manage app access, Premium, Voice and Game Booster controls.</small><span className="tile-cta">OPEN APP CENTER ↗</span></button>
   </div>
   <div className="picker-foot"><span>Ⓡ OSEN AUREX</span><span>ONE OWNER · TWO WORKSPACES</span><button onClick={logout}>LOG OUT</button></div>
  </section>:<div className="nexus-workspace">
   <aside className="side-nav"><button className="workspace-switch" onClick={goHome}>← ALL WORKSPACES</button><div className="nav-caption">{workspace==="website"?"WEBSITE CENTER":"ANDROID APP CENTER"}</div>{currentNav.map(([id,label,ico])=><a key={id} href={"#"+id} aria-current={activeSection===id?"page":undefined} className={activeSection===id?"nav-active":""} onClick={e=>{e.preventDefault();navigateSection(id)}}><span>{ico}</span>{label}</a>)}<div className="sidebar-bottom"><i/> SYSTEM READY<small>Owner-only workspace</small></div></aside>
   <div className="dashboard-content" data-active={activeSection} data-workspace={workspace}>
    <section id="overview" className="welcome-panel"><div className="workspace-context">{workspace==="website"?"WEBSITE CENTER":"ANDROID APP CENTER"}</div><div><div className="section-kicker">CONTROL CENTER　/　{(currentNav.find(([id])=>id===activeSection)?.[1]||"OVERVIEW").toUpperCase()}</div><h1>{activeSection==="overview"?"Welcome back,":activeSection==="premium"?"Premium":"Manage"} <em>{activeSection==="overview"?OWNER:activeSection==="premium"?"Access":" "+(currentNav.find(([id])=>id===activeSection)?.[1]||"Workspace")}.</em></h1><p>{activeSection==="premium"?"Manage global Premium availability and securely update the app verification password.":activeSection==="overview"?"Your private workspace to manage OSEN apps, access and website projects.":`Manage ${currentNav.find(([id])=>id===activeSection)?.[1]||"workspace"} settings from your private OSEN AUREX console.`}</p></div><div className="welcome-status"><i/> PRIVATE WORKSPACE</div></section>
    <div className="metric-grid">
     <article className="metric-card metric-gold"><span>PREMIUM SYSTEM</span><strong className={enabled?"status-on":"status-off"}>{enabled?"ENABLED":"DISABLED"}</strong><small>Global Premium access</small><button className="primary-btn" disabled={busy} onClick={()=>run("set_premium_enabled",{enabled:!enabled},enabled?"Premium access disabled.":"Premium access enabled.")}>{enabled?"TURN OFF PREMIUM":"TURN ON PREMIUM"}</button></article>
     <article className="metric-card"><span>PREMIUM ACCESS RECORDS</span><strong>{users.length}</strong><small>{premiumCount} currently approved · {users.length-premiumCount} not approved</small><a className="text-link" href="#users" onClick={e=>{e.preventDefault();navigateSection("users")}}>Manage users　↗</a></article>

     <article className="metric-card"><span>MAINTENANCE MODE</span><strong className={maintenance?"status-off":"status-on"}>{maintenance?"ON":"OFF"}</strong><small>Remote setting · app integration pending</small><a className="text-link" href="#app-control" onClick={e=>{e.preventDefault();navigateSection("app-control")}}>App settings　↗</a></article>
    </div>
    <div className="content-grid">
     <section id="premium" className="panel-card"><div className="panel-heading"><div><span className="section-kicker">SECURITY CONTROL</span><h2>Premium <em>password</em></h2></div><span className="panel-icon">◇</span></div><p>Set or replace the password required by the app. The server stores only a secure password hash, so the existing password cannot be displayed here.</p><div className="integration-note"><b>PASSWORD STATUS</b><span>{passwordChangedAt?`Last changed · ${new Date(passwordChangedAt).toLocaleString()}`:"No password change recorded yet"}</span></div><div className="inline-form"><input type={showNewPass?"text":"password"} autoComplete="new-password" minLength={8} maxLength={128} value={newPass} onChange={e=>setNewPass(e.target.value)} placeholder="New password · 8–128 characters"/><input type={showNewPass?"text":"password"} autoComplete="new-password" minLength={8} maxLength={128} value={newPassConfirm} onChange={e=>setNewPassConfirm(e.target.value)} placeholder="Confirm new password"/><button className="outline-btn" type="button" disabled={!newPass&&!newPassConfirm} onClick={()=>setShowNewPass(v=>!v)}>{showNewPass?"HIDE PASSWORD":"SHOW PASSWORD"}</button><button disabled={busy||newPass.length<8||newPass!==newPassConfirm} onClick={async()=>{const ok=await run("change_premium_password",{password:newPass},"Premium password updated successfully.");if(ok){setNewPass("");setNewPassConfirm("");setShowNewPass(false)}}}>UPDATE PASSWORD</button></div>{newPassConfirm&&newPass!==newPassConfirm?<small className="status-off">Passwords do not match.</small>:newPass.length>0&&newPass.length<8?<small className="status-off">Use at least 8 characters.</small>:null}</section>
     <section id="app-control" className="panel-card"><div className="panel-heading"><div><span className="section-kicker">GLOBAL APP CONTROL</span><h2>Remote <em>configuration</em></h2></div><span className="panel-icon">⚙</span></div><p>These values are saved centrally. The Android app must fetch this configuration before it can act on it.</p><div className="setting-row"><div><b>Maintenance mode</b><small>Show maintenance state in the app after integration</small></div><strong className={maintenance?"status-off":"status-on"}>{maintenance?"ON":"OFF"}</strong><button className="outline-btn" disabled={busy} onClick={()=>run("set_app_config",{maintenance_mode:!maintenance})}>{maintenance?"TURN OFF":"TURN ON"}</button></div><div className="setting-row"><div><b>Force update notice</b><small>Update warning after Android integration</small></div><strong className={forceUpdate?"status-on":"status-off"}>{forceUpdate?"ON":"OFF"}</strong><button className="outline-btn" disabled={busy} onClick={()=>run("set_app_config",{force_update:!forceUpdate})}>{forceUpdate?"TURN OFF":"TURN ON"}</button></div><div className="inline-form"><input maxLength={500} value={announcement} onChange={e=>setAnnouncement(e.target.value)} placeholder="Announcement for app users · max 500 characters"/><button disabled={busy} onClick={()=>run("set_app_config",{announcement},"Announcement saved.")}>SAVE NOTICE</button></div></section>
     <section id="users" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">ACCESS MANAGEMENT</span><h2>Premium <em>users</em></h2></div><span className="count-pill">{users.length} RECORDS</span></div><p>Grant or revoke Premium access using the exact app user ID. This list is not yet a complete list of every installed device.</p><div className="inline-form"><input value={newId} onChange={e=>setNewId(e.target.value)} placeholder="App user ID"/><button disabled={busy||!newId.trim()} onClick={()=>run("set_user_premium",{user_id:newId.trim(),premium:true},"Premium access granted.")}>GRANT ACCESS</button></div><div className="table-tools"><span>USER ACCESS LIST</span><input className="search-input" value={userSearch} onChange={e=>setUserSearch(e.target.value)} placeholder="Search user ID"/></div><div className="data-list">{filteredUsers.length?filteredUsers.map(u=><div className="data-row" key={u.user_id}><code>{u.user_id}</code><span className={u.premium?"status-on":"status-off"}>{u.premium?"PREMIUM":"FREE"}</span><small>{u.updated_at?new Date(u.updated_at).toLocaleDateString():"—"}</small><button className="outline-btn" disabled={busy} onClick={()=>run("set_user_premium",{user_id:u.user_id,premium:!u.premium},u.premium?"Access revoked.":"Access granted.")}>{u.premium?"REVOKE":"GRANT"}</button><button className="danger-btn" disabled={busy} onClick={()=>{if(window.confirm("Remove this user access record?"))run("delete_premium_user",{user_id:u.user_id},"User record removed.")}}>REMOVE</button></div>):<div className="empty-state">No matching user records.</div>}</div></section>
     {workspace==="app"&&<section id="app-projects" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">ANDROID APP PROJECTS</span><h2>App <em>projects</em></h2></div><span className="panel-icon">▣</span></div><p>Create and track Android app project records here. APK/AAB compilation and store publishing need a connected build service and signing configuration.</p><div className="inline-form"><input maxLength={80} value={appName} onChange={e=>setAppName(e.target.value)} placeholder="Android app project name"/><button disabled={busy||!appName.trim()} onClick={async()=>{const ok=await run("create_app_project",{name:appName.trim()},"Android app project created.");if(ok)setAppName("")}}>＋ CREATE APP PROJECT</button></div><div className="table-tools"><span>YOUR ANDROID PROJECTS</span><input className="search-input" value={appSearch} onChange={e=>setAppSearch(e.target.value)} placeholder="Search app projects"/></div><div className="data-list">{filteredAppProjects.length?filteredAppProjects.map(p=><div className="data-row site-row" key={p.id}><b>{p.name}</b><code>{p.package_name||"Package name not set"}</code><span className="draft-pill">{p.status.toUpperCase()}</span><small>{new Date(p.created_at).toLocaleDateString()}</small></div>):<div className="empty-state">No app projects yet. Create your first Android project above.</div>}</div><div className="integration-note"><b>ANDROID BUILD STATUS</b><span>APK / AAB build service · Not connected</span><span>Signing key · Not configured</span><span>Store publishing · Not connected</span></div></section>}
     <section id="websites" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">WEBSITE STUDIO · BUILDER</span><h2>Build your <em>website</em></h2></div><span className="panel-icon">◎</span></div><p>Create a project, choose a starter layout, edit the page content and preview it before saving. Publishing and custom domains become available after hosting is connected.</p><div className="site-builder-create"><input maxLength={80} value={siteName} onChange={e=>setSiteName(e.target.value)} placeholder="Website / project name"/><div className="site-template-options"><button type="button" className={siteTemplate==="portfolio"?"template-choice selected":"template-choice"} onClick={()=>setSiteTemplate("portfolio")}><b>◈ Portfolio</b><small>Personal work & projects</small></button><button type="button" className={siteTemplate==="business"?"template-choice selected":"template-choice"} onClick={()=>setSiteTemplate("business")}><b>▦ Business</b><small>Company & services</small></button><button type="button" className={siteTemplate==="landing"?"template-choice selected":"template-choice"} onClick={()=>setSiteTemplate("landing")}><b>✦ Product Landing</b><small>Promote one product</small></button><button type="button" className={siteTemplate==="creator"?"template-choice selected":"template-choice"} onClick={()=>setSiteTemplate("creator")}><b>◎ Creator</b><small>Social & creator profile</small></button></div><button className="site-create-submit" disabled={busy||!siteName.trim()} onClick={async()=>{const ok=await run("create_site_project",{name:siteName.trim(),template:siteTemplate},"Website project created.");if(ok){setSiteName("");setActiveSiteId("")}}}>＋ CREATE WEBSITE</button></div><div className="table-tools"><span>YOUR WEBSITE PROJECTS</span><input className="search-input" value={siteSearch} onChange={e=>setSiteSearch(e.target.value)} placeholder="Search projects"/></div><div className="data-list">{filteredSites.length?filteredSites.map(s=><div className="data-row site-row" key={s.id}><b>{s.name}</b><code>{s.domain||"Domain not connected"}</code><span className="draft-pill">{s.status.toUpperCase()}</span><button className="outline-btn" disabled={busy} onClick={()=>{setActiveSiteId(s.id);setSiteTemplate(s.template||"portfolio");setSiteHeadline(s.content?.headline||"Build something remarkable.");setSiteDescription(s.content?.description||"Introduce your work, your ideas and what makes your project unique.");setSiteButtonText(s.content?.button_text||"Get started");setSiteAccent(s.content?.accent||"#d8b15e");setPreviewOpen(false)}}>EDIT</button><button className="outline-btn" disabled={busy||!["draft","paused"].includes(s.status)} onClick={()=>run("set_site_status",{id:s.id,status:s.status==="paused"?"draft":"paused"},s.status==="paused"?"Project resumed.":"Project paused.")}>{s.status==="paused"?"RESUME":"PAUSE"}</button><button className="danger-btn" disabled={busy} onClick={()=>{if(window.confirm("Delete this project record?"))run("delete_site_project",{id:s.id},"Project record deleted.")}}>DELETE</button></div>):<div className="empty-state">No websites yet. Create your first website above.</div>}</div>{activeSiteId&&<div className="site-editor"><div className="panel-heading"><div><span className="section-kicker">PAGE EDITOR</span><h2>{sites.find(s=>s.id===activeSiteId)?.name||"Website"} <em>content</em></h2></div><button className="outline-btn" onClick={()=>setActiveSiteId("")}>CLOSE EDITOR</button></div><label>Hero headline</label><input className="site-editor-input" maxLength={100} value={siteHeadline} onChange={e=>setSiteHeadline(e.target.value)}/><label>Short description</label><textarea className="site-editor-input" maxLength={500} rows={3} value={siteDescription} onChange={e=>setSiteDescription(e.target.value)}/><label>Button text</label><input className="site-editor-input" maxLength={32} value={siteButtonText} onChange={e=>setSiteButtonText(e.target.value)}/><label>Accent color</label><div className="site-color-row"><input type="color" value={siteAccent} onChange={e=>setSiteAccent(e.target.value)}/><code>{siteAccent}</code><span>Template: {siteTemplate}</span></div><div className="site-editor-actions"><button className="outline-btn" onClick={()=>setPreviewOpen(v=>!v)}>{previewOpen?"HIDE PREVIEW":"◉ PREVIEW DESIGN"}</button><button className="site-save-btn" disabled={busy||!siteHeadline.trim()} onClick={()=>run("save_site_content",{id:activeSiteId,template:siteTemplate,content:{headline:siteHeadline,description:siteDescription,button_text:siteButtonText,accent:siteAccent}},"Website draft saved.")}>SAVE DRAFT</button></div>{previewOpen&&<div className="site-preview-shell"><div className="site-preview-toolbar"><b>LIVE DESIGN PREVIEW</b><div><button type="button" className={sitePreviewDevice==="desktop"?"preview-device active":"preview-device"} onClick={()=>setSitePreviewDevice("desktop")}>▭ DESKTOP</button><button type="button" className={sitePreviewDevice==="mobile"?"preview-device active":"preview-device"} onClick={()=>setSitePreviewDevice("mobile")}>▯ MOBILE</button></div></div><div className={"site-live-preview template-"+siteTemplate+" preview-"+sitePreviewDevice} style={{borderColor:siteAccent,"--site-accent":siteAccent} as React.CSSProperties}><div className="preview-nav"><b>{sites.find(s=>s.id===activeSiteId)?.name||"YOUR BRAND"}</b><span>HOME　 ABOUT　 CONTACT</span></div><div className="preview-hero"><span className="preview-eyebrow" style={{color:siteAccent}}>{siteTemplate==="business"?"WELCOME TO OUR COMPANY":siteTemplate==="landing"?"INTRODUCING SOMETHING NEW":siteTemplate==="creator"?"CREATOR · PORTFOLIO":"SELECTED WORK & IDEAS"}</span><h2>{siteHeadline}</h2><p>{siteDescription}</p><button style={{background:siteAccent,color:"#111"}}>{siteButtonText}</button></div><div className="preview-feature-row"><span>01　Thoughtful design</span><span>02　Made with purpose</span><span>03　Built for you</span></div><small className="preview-disclaimer">PREVIEW ONLY · NOT PUBLISHED</small></div></div>}<div className="integration-note"><b>HOSTING STATUS</b><span>Vercel · Not connected</span><span>Custom domain · Not connected</span><span>Publish · Waiting for hosting setup</span></div></section>
     <section id="activity" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">SECURITY & TRANSPARENCY</span><h2>Admin <em>activity log</em></h2></div><span className="count-pill">LATEST 25</span></div><p>Recent administrative actions recorded by the server.</p><div className="data-list">{logs.length?logs.map(log=><div className="log-row" key={log.id}><span className="log-dot"/><div><b>{log.action.replaceAll("_"," ").toUpperCase()}</b><small>{log.actor_email} · {new Date(log.created_at).toLocaleString()}</small></div></div>):<div className="empty-state">No activity recorded yet.</div>}</div></section>
     <section id="integrations" className="panel-card span-two"><div className="panel-heading"><div><span className="section-kicker">PLATFORM INTEGRATIONS</span><h2>Owner <em>tools</em></h2></div><span className="panel-icon">⌘</span></div><div className="integration-list"><div><b>Android device registry</b><span>Pending · app must register a generated device UUID</span></div><div><b>Voice & Booster remote controls</b><span>Pending · Android app must fetch and apply remote settings</span></div><div><b>Vercel hosting & custom domains</b><span>Pending · provider API credentials and project linking required</span></div><div><b>Audit trail</b><span className="ready-label">ACTIVE · admin actions are logged</span></div></div></section>
    </div>
    {notice&&<div className={noticeError?"alert error":"alert"} role="status">{notice}</div>}
    <footer className="nexus-footer"><span>OSEN AUREX　·　OWNER CONSOLE</span><span>BLACK / SKY BLUE / GOLD　·　{OWNER}</span></footer>
   </div>
  </div>}
 </main>;
}
