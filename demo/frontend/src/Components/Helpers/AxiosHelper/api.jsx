import axios from "axios";
import { setServerDown } from "./ServerStatus";

const api = axios.create({
    baseURL: "http://localhost:8080",
    timeout: 5000,
    withCredentials: true
});

// REQUEST
api.interceptors.request.use(config => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// RESPONSE
api.interceptors.response.use(
    response => response,
    error => {
        // сервер не отвечает
        if (!error.response) {
            setServerDown();
        }

        // backend сказал: я умер
        if (error.response?.status === 503) {
            setServerDown();
        }

        return Promise.reject(error);
    }
);

export default api;