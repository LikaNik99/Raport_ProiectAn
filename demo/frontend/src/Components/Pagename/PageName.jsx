import * as React from "react";
import "./PageName.css"

export default function PageName(props){
    return(
        <div>
            <h1 className="header-name">
                {props.name}
            </h1>
        </div>
    )
}
