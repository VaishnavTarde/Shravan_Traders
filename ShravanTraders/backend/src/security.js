const jwt = require('jsonwebtoken');
function auth(req,res,next){
 const header=req.headers.authorization||''; const token=header.startsWith('Bearer ')?header.slice(7):'';
 if(!token) return res.status(401).json({message:'Please log in.'});
 try { req.auth=jwt.verify(token,process.env.JWT_SECRET); next(); }
 catch { return res.status(401).json({message:'Session expired. Please log in again.'}); }
}
const ownerOnly=(req,res,next)=>req.auth?.role==='owner'?next():res.status(403).json({message:'Owner access required.'});
module.exports={auth,ownerOnly};
