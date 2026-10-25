# Shravan Traders — Android + Node.js + MongoDB Atlas

A starter full-stack shop-management project for Shravan Traders. Includes Android Java/XML app, Express backend, MongoDB Atlas connection, owner/customer roles, stock/products, sales, customers, reports, Excel export, JSON backup/restore, Cloudinary signed-upload support, and optional Gemini proxy endpoint.

## Important honest status
This project is supplied as a configurable starter, not a guarantee of production certification. You must add your own secrets and test on your machine. An Android build cannot be verified here without your Android SDK/Gradle environment and API credentials. Backend syntax and dependency structure are provided; run the checks below before real shop use.

## 1. Backend setup
Requirements: Node.js 20+ and npm.

1. Open `backend/` in terminal.
2. Copy `.env.example` to `.env` and fill in values.
3. Run `npm install`
4. Run `npm run check`
5. Run `npm start`
6. Check `http://localhost:5000/api/health`

Set MongoDB Atlas Network Access to your development IP and create a least-privilege database user. Do not use `0.0.0.0/0` permanently. Use HTTPS for deployed API. Keep `.env` out of Git.

### Create the owner account
Set a long random `OWNER_SETUP_KEY` in `.env`. Send one POST request to `/api/setup/owner` with header `x-owner-setup-key: YOUR_SETUP_KEY` and JSON body:
`{"name":"Vinayak Bhau Tarade","mobile":"YOUR_MOBILE","password":"A_STRONG_PASSWORD"}`
This endpoint is one-time: it refuses to create another owner if an owner already exists. Remove/rotate `OWNER_SETUP_KEY` after setup. Do not put owner credentials in source code.

### Android API URL
For Android Emulator, `http://10.0.2.2:5000` points to your computer. Set `API_BASE_URL` in `android-app/app/build.gradle` for local development. For a physical phone, use your computer's LAN IP while on the same Wi-Fi. For production use a deployed HTTPS backend URL.

Android cleartext HTTP is enabled only to support local development. For production, disable cleartext traffic and use HTTPS.

## 2. Android Studio
Open the `android-app` folder (not the outer ZIP folder) in Android Studio. Allow Gradle sync. Use JDK 17 and Android SDK 35. Run on an emulator or device. Backend must be reachable from the device.

## 3. Environment variables
- `MONGODB_URI`: Atlas connection string. Encode special characters in DB password.
- `JWT_SECRET`: long random secret, unique per environment.
- `OWNER_SETUP_KEY`: temporary owner bootstrap secret.
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`: optional signed product-image upload.
- `GEMINI_API_KEY`: optional; never place it in Android code.

Cloudinary signing endpoint returns signature parameters only; upload image bytes from the client directly to Cloudinary using those signed values. Keep Cloudinary API secret on backend.

## Security notes
- Passwords are hashed with bcrypt; API uses JWT and role middleware.
- Customer routes scope records to the authenticated customer's ID.
- Owner-only endpoints enforce role on the server, not only in UI.
- Use HTTPS, rotate secrets, rate-limit production endpoints, configure CORS to known origins, enable Atlas backups, and restrict database permissions before production.
- Backup export is owner-only. Restore is deliberately not exposed as a public endpoint; store downloaded encrypted backups securely and restore via a reviewed admin maintenance process. Do not rely on a single backup.
- Local offline sync is not included in this first version; online API access is required for current records.
- Gemini is optional and is not used for financial calculations. All totals are computed by backend code.

## Implemented API outline
`POST /api/auth/login`, `GET /api/auth/me`, `POST /api/setup/owner`, owner CRUD products/customers, sales creation/listing, customer self profile/bills, reports, Excel workbook, JSON backup, Cloudinary signature, optional Gemini business summary.

## Run backend checks
`npm run check` performs syntax checks. Test with Postman/curl and a non-production database before entering real shop data.
