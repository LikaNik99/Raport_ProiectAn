import axios from "axios";

axios.defaults.baseURL = "http://localhost:8080";
axios.defaults.headers.post["Content-Type"] = "application/json";

export const getAuthToken = () => {
    return localStorage.getItem("token");
};

export const request = (method, url, data) => {
    const token = getAuthToken();

    const headers = token
        ? { Authorization: `Bearer ${token}` }
        : {};
    return axios({
        method,
        url,
        data,
        headers
    });
};
