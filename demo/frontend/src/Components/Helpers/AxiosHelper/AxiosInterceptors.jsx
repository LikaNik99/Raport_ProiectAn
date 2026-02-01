import axios from "axios";
// import { useAuth } from "./../../../Security/AuthContext";
export const setupAxiosInterceptors = (logout) => {
    axios.interceptors.response.use(
        res => res,
        err => {
            if (err.response?.status === 401) {
                logout();
            }
            return Promise.reject(err);
        }
    );
};