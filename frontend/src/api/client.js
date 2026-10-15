import axios from 'axios';
const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
});
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('mentivo_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
api.interceptors.response.use((response) => response, (error) => {
    if (error.response && error.response.status === 401) {
        // Token expired or invalid
        if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register')) {
            localStorage.removeItem('mentivo_token');
            localStorage.removeItem('mentivo_user');
        }
    }
    return Promise.reject(error);
});
export default api;
