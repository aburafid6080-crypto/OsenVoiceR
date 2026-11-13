import { NextRequest, NextResponse } from "next/server";
import { pbkdf2Sync, randomBytes } from "node:crypto";
export const runtime="nodejs";
const rawUrl=process.env.SUPABASE_URL||process.env.NEXT_PUBLIC_SUPABASE_URL||"";
const url=(()=>{try{const p=new URL(rawUrl.trim());return p.protocol+"//"+p.host}catch{return rawUrl.trim().replace(/\\/+$/,"")}})();
const anon=process.env.SUPABASE_ANON_KEY||process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY||"";
const service=process.env.SUPABASE_SERVICE_ROLE_KEY||"";
const owner=(process.env.ADMIN_EMAIL||"").toLowerCase();
function config(){if(!url||!anon||!service||!owner)throw new Error("Server environment is incomplete. Configure Supabase URL, anon key, service role key and owner email.");}
async function authorize(req:NextRequest){
 config();const token=req.headers.get("authorization")?.replace(/^Bearer\\s+/i,"");if(!token)throw new Error("Sign in required.");
 const response=await fetch(url+"/auth/v1/user",{headers:{apikey:anon,Authorization:"Bearer "+token},cache:"no-store"});if(!response.ok)throw new Error("Invalid login session.");
 const user=await response.json();if(String(user.email||"").toLowerCase()!==owner)throw new Error("Owner access only.");return user;
}
async function db(path:string,init:RequestInit={}){
 const response=await fetch(url+"/rest/v1/"+path,{...init,headers:{apikey:service,Authorization:"Bearer "+service,"Content-Type":"application/json",Prefer:"return=representation",...(init.headers||{})},cache:"no-store"});
 const body=await response.text();if(!response.ok)throw new Error("Database request failed: "+body.slice(0,220));return body?JSON.parse(body):null;
}
async function audit(user:any,action:string,details:Record<string,unknown>={}){
 try{await db("admin_audit_logs",{method:"POST",body:JSON.stringify({actor_email:String(user.email||owner),action,details})});}catch{/* Keep the main action successful if optional audit logging is unavailable. */}
}
export async function GET(req:NextRequest){
 try{
  await authorize(req);
  const [settings,users,configs,sites,logs]=await Promise.all([
   db("app_settings?select=premium_enabled&id=eq.main"),
   db("premium_users?select=user_id,premium,updated_at&order=updated_at.desc"),
   db("app_config?select=maintenance_mode,force_update,announcement&id=eq.main").catch(()=>[]),
   db("website_projects?select=id,name,domain,status,created_at&order=created_at.desc").catch(()=>[]),
   db("admin_audit_logs?select=id,actor_email,action,details,created_at&order=created_at.desc&limit=25").catch(()=>[])
  ]);
  return NextResponse.json({settings:settings?.[0]||{premium_enabled:false},users:users||[],config:configs?.[0]||{maintenance_mode:false,force_update:false,announcement:""},sites:sites||[],logs:logs||[]});
 }catch(error){return NextResponse.json({error:error instanceof Error?error.message:"Unauthorized"},{status:401});}
}
export async function POST(req:NextRequest){
 try{
  const user=await authorize(req);const body=await req.json();
  if(body.action==="set_premium_enabled"){
   if(typeof body.enabled!=="boolean")throw new Error("Invalid status.");
   await db("app_settings?id=eq.main",{method:"PATCH",body:JSON.stringify({premium_enabled:body.enabled})});
   await audit(user,"set_premium_enabled",{enabled:body.enabled});return NextResponse.json({ok:true});
  }
  if(body.action==="change_premium_password"){
   if(typeof body.password!=="string"||body.password.length<8||body.password.length>128)throw new Error("Password must be 8–128 characters.");
   const salt=randomBytes(16);const hash=pbkdf2Sync(body.password,salt,210000,32,"sha256");
   await db("app_settings?id=eq.main",{method:"PATCH",body:JSON.stringify({premium_password_salt:salt.toString("hex"),premium_password_hash:hash.toString("hex"),premium_password_iterations:210000,premium_password_changed_at:new Date().toISOString()})});
   await audit(user,"change_premium_password");return NextResponse.json({ok:true});
  }
  if(body.action==="set_user_premium"){
   if(typeof body.user_id!=="string"||!body.user_id.trim()||body.user_id.length>128||typeof body.premium!=="boolean")throw new Error("Enter a valid app user ID.");
   const id=body.user_id.trim();
   await db("premium_users?on_conflict=user_id",{method:"POST",headers:{Prefer:"resolution=merge-duplicates,return=representation"},body:JSON.stringify({user_id:id,premium:body.premium,updated_at:new Date().toISOString()})});
   await audit(user,"set_user_premium",{user_id:id,premium:body.premium});return NextResponse.json({ok:true});
  }
  if(body.action==="delete_premium_user"){
   if(typeof body.user_id!=="string"||!body.user_id.trim()||body.user_id.length>128)throw new Error("Enter a valid user ID.");
   const id=body.user_id.trim();await db("premium_users?user_id=eq."+encodeURIComponent(id),{method:"DELETE"});
   await audit(user,"delete_premium_user",{user_id:id});return NextResponse.json({ok:true});
  }
  if(body.action==="set_app_config"){
   const patch:Record<string,unknown>={};if(typeof body.maintenance_mode==="boolean")patch.maintenance_mode=body.maintenance_mode;
   if(typeof body.force_update==="boolean")patch.force_update=body.force_update;
   if(typeof body.announcement==="string"&&body.announcement.length<=500)patch.announcement=body.announcement;
   if(!Object.keys(patch).length)throw new Error("No valid settings supplied.");
   await db("app_config?id=eq.main",{method:"PATCH",body:JSON.stringify({...patch,updated_at:new Date().toISOString()})});
   await audit(user,"set_app_config",patch);return NextResponse.json({ok:true});
  }
  if(body.action==="create_site_project"){
   if(typeof body.name!=="string"||!body.name.trim()||body.name.trim().length>80)throw new Error("Project name must be 1–80 characters.");
   const created=await db("website_projects",{method:"POST",body:JSON.stringify({name:body.name.trim(),status:"draft"})});
   await audit(user,"create_site_project",{name:body.name.trim()});return NextResponse.json({ok:true,project:Array.isArray(created)?created[0]:created});
  }
  if(body.action==="set_site_status"){
   if(typeof body.id!=="string"||!/^[0-9a-f-]{36}$/i.test(body.id)||!["draft","paused"].includes(body.status))throw new Error("Invalid project status.");
   await db("website_projects?id=eq."+encodeURIComponent(body.id),{method:"PATCH",body:JSON.stringify({status:body.status,updated_at:new Date().toISOString()})});
   await audit(user,"set_site_status",{project_id:body.id,status:body.status});return NextResponse.json({ok:true});
  }
  if(body.action==="delete_site_project"){
   if(typeof body.id!=="string"||!/^[0-9a-f-]{36}$/i.test(body.id))throw new Error("Invalid project ID.");
   await db("website_projects?id=eq."+encodeURIComponent(body.id),{method:"DELETE"});
   await audit(user,"delete_site_project",{project_id:body.id});return NextResponse.json({ok:true});
  }
  return NextResponse.json({error:"Unknown action."},{status:400});
 }catch(error){return NextResponse.json({error:error instanceof Error?error.message:"Request failed"},{status:400});}
}
