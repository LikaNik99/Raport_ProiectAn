import * as React from "react";
import {request} from "../../Axios/axios_helper";

export default class AuthContent extends React.Component{

    constructor(props) {
        super(props);
        this.state = {
            data:[]
        }
    }
    componentDidMount() {
        request("GET", "/api/messages", {})
            .then((response) => {
                this.setState({data:response.data})
            });
    };

    render() {
        return(

            <div className="row justify-content-md-center">
                <div className="col-4">
                    <div className="card" style={{width: "18rem"}}>
                        <div className="card-body">
                            <h5 className="card-title">Backend Response</h5>
                            <p className="card-text"> Content : </p>

                            <ul>
                                {
                                    this.state.data && this.state.data.map((item, index) => <li key={index}>{item}</li>)
                                }
                            </ul>

                        </div>
                    </div>
                </div>

            </div>

        )
    }
}