import * as React from "react";
import PageName from "../Pagename/PageName";
import "./MyProjectsComponent.css"
import {Link, useNavigate} from "react-router-dom";
import NoDataMessage from "../Messages/NoDataMessage";
import {useEffect, useState} from "react";
import axios from "axios";
import {request} from "./../Helpers/AxiosHelper/AxiosHelper";


export default function MyProjects(props) {
    const [projects, setProject] = useState([])
    const token = localStorage.getItem("token");
    const navigate = useNavigate();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    useEffect(() => {
            request("GET", "/api/my/projects")
                .then((res) => {
                    setProject(res.data);
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
            <PageName name="My Projects"/>
            <div className="bublePageAdd">
                <Link to="/create-new-project">
                    <div className="bubleLink">
                        <p>
                            +
                        </p>
                    </div>
                </Link>
                <div className="container">
                <div className="table-wrapper">
                    <table>
                        <thead>
                        <tr>
                            <th>Project Name</th>
                            <th>Created on</th>
                            <th>Dead Line</th>
                            <th>Created By</th>
                            <th>Owner</th>
                            <th>Status</th>
                        </tr>
                        </thead>
                        <tbody>
                        {projects.length === 0 ? (
                            <tr className="messageRow">
                                <td colSpan={5}> No available projects or you dont have access to view..</td>
                            </tr>
                        ) : (
                            projects.map(project => {
                                return (
                                    <tr key={project.id}>
                                        <td style={{
                                            cursor: "pointer",
                                            color: "blue",
                                            textDecoration: "none",
                                            pointerEvents: "auto"
                                        }}
                                            onClick={() => {handleView(project.id); console.log(project.id)}}>
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

            
        </div>
    )
}