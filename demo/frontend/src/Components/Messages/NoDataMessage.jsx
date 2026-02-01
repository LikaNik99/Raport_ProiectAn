import React from "react";
import "./NoDataMessage.css"

export default function NoDataMessage(props){
    return(
        <div className={"outerDiv"}>
            <div className={"innerDiv"}>
                <p className={"messageContent"}>{props.message}</p>
            </div>
        </div>
    )
}
