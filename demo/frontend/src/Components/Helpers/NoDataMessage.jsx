import React from "react";


export default function NoDataMessage(props){
    return(
        <>
            <div className="no-data-message" style={{ padding: "10px", textAlign: "center" }}>
                <strong>{props.message}</strong>
            </div>
        </>
    )
}