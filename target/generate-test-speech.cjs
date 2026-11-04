const fs = require('node:fs');
const crypto = require('node:crypto');
const env = Object.fromEntries(fs.readFileSync('.env', 'utf8').split(/\r?\n/).filter(l => l && !l.startsWith('#')).map(l => { const n = l.indexOf('='); return [l.slice(0,n),l.slice(n+1)]; }));
async function run() {
 const text='I built a Java Spring Boot service for document search. I added validation and clear error messages. I measured success by checking how many failed requests remained after the change.';
 const response=await fetch('https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash-tts:generateContent', {method:'POST', headers:{'Content-Type':'application/json','x-goog-api-key':env.GEMINIAI_API_KEY},body:JSON.stringify({contents:[{parts:[{text:'Read this naturally and clearly: '+text}]}],generationConfig:{responseModalities:['AUDIO'],speechConfig:{voiceConfig:{prebuiltVoiceConfig:{voiceName:'Kore'}}}}}),signal:AbortSignal.timeout(90000)});
 const result=await response.json();
 if(!response.ok) throw new Error('Speech fixture API returned HTTP '+response.status);
 const inline=result.candidates?.[0]?.content?.parts?.find(p=>p.inlineData)?.inlineData;
 if(!inline?.data) throw new Error('No audio in speech response');
 let bytes=Buffer.from(inline.data,'base64'); let rate=Number(inline.mimeType?.match(/rate=(\d+)/)?.[1]||24000);
 if(bytes.toString('ascii',0,4)==='RIFF') { let pcm; for(let at=12;at+8<=bytes.length;){ const tag=bytes.toString('ascii',at,at+4),size=bytes.readUInt32LE(at+4); if(tag==='fmt ') rate=bytes.readUInt32LE(at+12); if(tag==='data') pcm=bytes.subarray(at+8,at+8+size); at+=8+size+(size%2); } if(!pcm) throw new Error('Missing WAV data'); bytes=pcm; }
 const wav=Buffer.alloc(44+bytes.length); wav.write('RIFF',0);wav.writeUInt32LE(36+bytes.length,4);wav.write('WAVEfmt ',8);wav.writeUInt32LE(16,16);wav.writeUInt16LE(1,20);wav.writeUInt16LE(1,22);wav.writeUInt32LE(rate,24);wav.writeUInt32LE(rate*2,28);wav.writeUInt16LE(2,32);wav.writeUInt16LE(16,34);wav.write('data',36);wav.writeUInt32LE(bytes.length,40);bytes.copy(wav,44);
 fs.writeFileSync('target/gemini-voice-check.wav',wav); console.log('Synthetic verification speech generated: '+(bytes.length/rate/2).toFixed(1)+' seconds.');
}
run().catch(e=>{console.error(e.message.replaceAll(env.GEMINIAI_API_KEY,'[redacted]'));process.exitCode=1;});
