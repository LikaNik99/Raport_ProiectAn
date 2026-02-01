import  api  from "../Helpers/AxiosHelper/Axios";

export default function TestRequest() {
    const callBackend = async () => {
        await api.get("/actuator/health");
    };

    return (
        <button onClick={callBackend}>
            TEST BACKEND
        </button>
    );
}