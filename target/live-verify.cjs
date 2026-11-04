const fs=require('node:fs'),crypto=require('node:crypto');
let token;
async function api(path,body,method='POST'){
 const response=await fetch('http://localhost:8081/api/v1'+path,{method,headers:{...(token?{Authorization:'Bearer '+token}:{}),...(body instanceof FormData?{}:{'Content-Type':'application/json'})},body:body===undefined?undefined:body instanceof FormData?body:JSON.stringify(body),signal:AbortSignal.timeout(180000)});
 if(!response.ok){const text=await response.text();throw new Error(path+' HTTP '+response.status+' '+text.slice(0,300));}
 return response;
}
async function run(){
 token=(await(await api('/auth/login',{username:'preproom_verify_0927',password:'Verify-Prep-0927!j8e'})).json()).accessToken;
 const form=new FormData();form.append('file',new Blob(['Beacon Test Company builds Java Spring Boot document search services. The team uses PostgreSQL and pgvector to retrieve relevant passages. Engineers must protect customer documents with per-user access controls, validate incoming uploads, and explain trade-offs clearly. The interview should explore idempotent APIs, database transactions, observability, and testing.'],{type:'text/plain'}),'verification-company.txt');
 const document=(await(await api('/documents/upload',form)).json()).data;
 if(document.status!=='INDEXED') throw new Error('Document was not indexed');console.log('PASS: live document parsing, Gemini embeddings, PGVector indexing.');
 let session=await(await api('/interviews',{jobRole:'Java / Spring Boot Developer',mode:'INTERVIEW',style:'TECHNICAL',level:'INTERMEDIATE',questionCount:3,documentId:document.id})).json();
 console.log('PASS: Gemini question with selected document source: '+session.transcripts[0].sources); console.log('Question: '+session.transcripts[0].content);
 fs.writeFileSync('target/live-verification-session.json',JSON.stringify({id:session.id,documentId:document.id}));
 const audio=new FormData();audio.append('file',new Blob([fs.readFileSync('target/gemini-voice-check.wav')],{type:'audio/wav'}),'answer.wav');audio.append('requestId',crypto.randomUUID());
 session=await(await api('/interviews/'+session.id+'/audio',audio)).json();
 const turn=session.transcripts.find(t=>t.speaker==='CANDIDATE');if(!turn?.audioFeedback)throw new Error('Missing audio coaching');
 console.log('PASS: live audio transcription and coaching. Transcript: '+turn.content);
 session=await(await api('/interviews/'+session.id+'/finish',{})).json();console.log('Feedback state: '+session.status);
 for(let n=0;n<36;n++){await new Promise(r=>setTimeout(r,2500));session=await(await api('/interviews/'+session.id,undefined,'GET')).json();if(session.status!=='ANALYZING')break;}
 if(session.status!=='ANALYZED')throw new Error('Feedback ended in '+session.status);
 console.log('PASS: live background feedback. '+session.feedback.summary);
 const report=await api('/interviews/'+session.id+'/report',undefined,'GET');fs.writeFileSync('target/live-verification-report.pdf',Buffer.from(await report.arrayBuffer()));
 console.log('PASS: saved report. Session id: '+session.id);
}
run().catch(e=>{console.error(e.message);process.exitCode=1;});
