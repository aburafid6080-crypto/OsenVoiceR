import { NextRequest, NextResponse } from "next/server";
export const runtime="nodejs";
const rawUrl=process.env.SUPABASE_URL||process.env.NEXT_PUBLIC_SUPABASE_URL||"";
const url=(()=>{try{return new URL(rawUrl.trim()).origin}catch{return rawUrl.trim()}})();
const anon=process.env.SUPABASE_ANON_KEY||process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY||"";
const service=process.env.SUPABASE_SERVICE_ROLE_KEY||"";
const owner=(process.env.ADMIN_EMAIL||"").toLowerCase();
function config(){if(!url||!anon||!service||!owner)throw new Error("Server environment is incomplete. Configure Supabase URL, anon key, service role key and owner email.");}
async function authorize(req:NextRequest){
 config();const auth=req.headers.get("authorization")||"";const token=auth.startsWith("Bearer ")?auth.slice(7):auth;if(!token)throw new Error("Sign in required.");
 const response=await fetch(url+"/auth/v1/user",{headers:{apikey:anon,Authorization:"Bearer "+token},cache:"no-store"});if(!response.ok)throw new Error("Invalid login session.");
 const user=await response.json();if(String(user.email||"").toLowerCase()!==owner)throw new Error("Owner access only.");return user;
}
async function db(path:string,init:RequestInit={}){
 const response=await fetch(url+"/rest/v1/"+path,{...init,headers:{apikey:service,Authorization:"Bearer "+service,"Content-Type":"application/json",Prefer:"return=representation",...(init.headers||{})},cache:"no-store"});
 const body=await response.text();if(!response.ok)throw new Error("Website database request failed: "+body.slice(0,220));return body?JSON.parse(body):null;
}
async function audit(user:any,action:string,details:Record<string,unknown>={}){
 try{await db("admin_audit_logs",{method:"POST",body:JSON.stringify({actor_email:String(user.email||owner),action,details})});}catch{/* Audit log is best-effort. */}
}
export async function GET(req:NextRequest){
 try{
  await authorize(req);
  const sites=await db("website_projects?select=id,name,domain,status,created_at&order=created_at.desc").catch(()=>[]);
  return NextResponse.json({sites:sites||[]});
 }catch(error){return NextResponse.json({error:error instanceof Error?error.message:"Unauthorized"},{status:401});}
}
export async function POST(req:NextRequest){
 try{
  const user=await authorize(req);const body=await req.json();
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
  return NextResponse.json({error:"Unknown Website Studio action."},{status:400});
 }catch(error){return NextResponse.json({error:error instanceof Error?error.message:"Request failed"},{status:400});}
}
