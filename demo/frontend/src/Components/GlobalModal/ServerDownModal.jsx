import { useEffect, useState } from "react";
import { subscribeServerStatus } from "../Helpers/AxiosHelper/ServerStatus";

export default function ServerDownModal() {
    const [down, setDown] = useState(false);

    useEffect(() => {
        console.log("🟢 ServerDownModal mounted");
        return subscribeServerStatus(setDown);
    }, []);

    if (!down) return null;

    return (
        <div style={backdrop}>
            <div style={modal}>
                <h2>Internal Server <b  style={{fontSize: "30px", color: "red"}}>Erorr </b>, Momentan nu este accesibil Serverul</h2>
                <p>
                    Pierdut conexiunea cu serverul sau baza de date <br/>
                    <b  style={{fontSize: "30px", color: "red"}}>Ne cerem scuze pentru deranj și vă rugăm să încercați mai târziu. </b>
                </p>
            </div>
        </div>
    );
}

const backdrop = {
    position: "fixed",
    inset: 0,
    background: "rgba(0,0,0,0.6)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 9999
};

const modal = {
    background: "#fff",
    padding: "30px",
    borderRadius: "8px",
    width: "400px",
    textAlign: "center"
};
