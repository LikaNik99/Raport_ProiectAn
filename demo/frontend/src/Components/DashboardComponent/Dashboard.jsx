import * as React from "react";
import {useEffect, useState} from "react";
import axios from "axios";
import "./Dashboard.css"
import PageName from "../Pagename/PageName";
import NoDataMessage from "../Messages/NoDataMessage";
import {request} from "../Helpers/AxiosHelper/AxiosHelper";
import {Button} from "react-bootstrap";
import {useNavigate} from "react-router-dom";
export default function Dashboard(props) {
    const [projects, setProjects] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        request("GET", "/api/projects")
            .then((res) => {
                setProjects(res.data);
                setLoading(false);
                console.log(res.data);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load projects data");
                setLoading(false);
            });
    }, []);

    const handleView = (projectId) => {
        navigate(`/project/${projectId}`); // make sure you have this route in React Router
    };

    return (
        <div>
            <PageName name="Dashboard"/>

            <div className="container">
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Project Name</th>
                            <th>Created on</th>
                            <th>Dead Line</th>
                            <th>Owner</th>
                            <th>Assigned To</th>
                            <th>Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        {projects.length === 0 ? (
                            <tr className={"messageRow"}>
                                <td colSpan={5}>
                                    {/*<NoDataMessage message="No available projects or you dont have access to view.."/>*/}
                                    No available projects or you dont have access to view..
                                </td>
                            </tr>
                        ) : (
                            projects.map(project => {
                                return (
                                    <tr key={project.id}>
                                        <td style={{
                                            cursor: "pointer",
                                            color: "blue",
                                            textDecoration: "none"
                                        }}
                                            onClick={() => handleView(project.id)}>
                                            {project.projectName}

                                        </td>
                                        <td>{project.createdOn}</td>
                                        <td>{project.deadLine}</td>
                                        <td>{project.creatorName}</td>
                                        <td>{project.owner}</td>
                                        <td>{project.status}</td>
                                    </tr>
                                )})
                        )}
                        </tbody>
                    </table>
                </div>
            </div>


        </div>


    )
}