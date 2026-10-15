# Mentivo Frontend (React + JavaScript + Vite)

A modern, high-performance two-sided knowledge marketplace frontend built using **React 19**, **JavaScript (JSX)**, **Tailwind CSS v4**, and **Vite**.

## Tech Stack
* **Framework:** React 19 (JSX)
* **Build Tool:** Vite 8
* **Styling:** Tailwind CSS v4
* **Routing:** React Router v7
* **Icons:** Lucide React
* **Networking & WebSockets:** Axios, STOMP.js, SockJS

## Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Start Development Server
```bash
npm run dev
```
The application will be running at `http://localhost:5173`.
All `/api` REST requests and `/ws` WebSocket requests are automatically proxied to the backend at `http://localhost:8080`.

### 3. Production Build
```bash
npm run build
```
Creates an optimized production bundle in the `dist/` directory.
