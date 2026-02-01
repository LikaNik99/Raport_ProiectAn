import axios from "axios";
import { setServerDown } from "./ServerStatus";


const api = axios.create({
    baseURL: "http://localhost:8080",
    withCredentials: true,
    timeout: 5000
});

//REQUEST
api.interceptors.request.use(config => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});



//RESPONSE
api.interceptors.response.use(
   response => response,
    error => {
        // сервер не отвечает вообще
        if (!error.response) {
            setServerDown();
        }

        // backend явно сказал: я умер
        if (error.response?.status === 503) {
            setServerDown();
        }

        // 401 — это не падение сервера, не путай
        if (error.response?.status === 401) {
            localStorage.removeItem("token");
        }

        return Promise.reject(error);
    }
)




export default api;