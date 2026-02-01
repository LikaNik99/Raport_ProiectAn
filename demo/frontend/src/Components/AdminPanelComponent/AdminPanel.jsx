import React from "react";
import PageName from "../Pagename/PageName";
import {Tab, Table, Tabs} from "react-bootstrap";
import {useEffect, useState} from "react";
import {request} from "../Helpers/AxiosHelper/AxiosHelper";
import {Button} from "react-bootstrap";
import NoDataMessage from "../Helpers/NoDataMessage";
import {useNavigate, useParams} from "react-router-dom";
export default props => {
    const [crmUsers, setCrmUsers] = useState([]);
    const [customers, setCustomers] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const navigate = useNavigate();
    const [config, setConfig] = useState([])
    const [projects, setProjects] = useState([])
    const {projectId} = useParams();



    /**
     * ------------------------------------------------------------------------------------------------------------
     * ------------------------------Admin Side
     * ------------------------------Application Properties section
     */
    useEffect(() => {
        request("GET", "/api/config")
            .then(res => setConfig(res.data))
            .catch(err => console.error(err));
    }, []);
    const handleSave = () => {
        request("PUT", "/api/config", config)
            .then(res => alert("Saved"))
            .catch(err => alert("Error"));
    };
    /**
     * ------------------------------ Completed Application Properties section
     * ------------------------------------------------------------------------------------------------------------
     */

    /**
     * ------------------------------------------------------------------------------------------------------------
     * ------------------------------Admin Side
     * ------------------------------Users and Customers section
     */

    useEffect(() => {
        request("GET", "/api/users")
            .then((res) => {
                setCrmUsers(res.data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load user data");
                setLoading(false);
            });
    }, []);

    useEffect(() => {
        request("GET", "/api/customers")
            .then((res) => {
                setCustomers(res.data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load user data");
                setLoading(false);
            });
    }, []);

    // Delete a user
    const handleDelete = async (userId) => {
        if (!window.confirm("Are you sure you want to delete this user?")) return;

        try {
            await request("DELETE", `/api/users/${userId}`);
            // Remove from state
            setCrmUsers((prev) => prev.filter((u) => u.id !== userId));
        } catch (err) {
            console.error(err);
            alert("Failed to delete user");
        }
    };

    // Navigate to user details page
    const handleView = (userId) => {
        navigate(`/users/${userId}`); // make sure you have this route in React Router
    };

    // Navigate to edit page
    const handleEdit = (userId) => {
        navigate(`/users/${userId}/edit`); // edit page route
    };
    /**
     * ------------------------------ Completed Users And Customers Section
     * ------------------------------------------------------------------------------------------------------------
     */

    /**
     * ------------------------------------------------------------------------------------------------------------
     * ------------------------------Admin Side-------------------------------------------------------------------
     * ------------------------------Projects section ----------------------------------------------------------
     */
    useEffect(() => {
        request("GET", "/api/projects")
            .then((res) => {
                setProjects(res.data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load projects data");
                setLoading(false);
            });
    }, []);

    // Navigate to user details page
    const handleViewProj = (projectId) => {
        navigate(`/project/${projectId}`); // make sure you have this route in React Router
    };

    const handleDeleteProj = async (projId) => {
        if (!window.confirm("Are you sure you want to delete this Project?")) return;

        try {
            await request("DELETE", `/api/project-delete/${projId}`);
            // Remove from state
            setCrmUsers((prev) => prev.filter((u) => u.id !== projId));
        } catch (err) {
            console.error(err);
            alert("Failed to delete Project");
        }
    };
    /**
     * ------------------------------ Completed Projects section --------------------------------------------------
     * ------------------------------------------------------------------------------------------------------------
     */

    /**
     * ------------------------------------------------------------------------------------------------------------
     * ------------------------------ Admin Side
     * ------------------------------ Messages section
     */

    const [messages, setMessages] = useState([])
    useEffect(() => {
        request("GET", "/api/project-messages")
            .then((res) => {
                console.log("Messages response", res)
                setMessages(Array.isArray(res.data) ? res.data : []);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load Messages data");
                setLoading(false);
            });
    }, []);

    /**
     * ------------------------------ Completed Messages Section
     * ------------------------------------------------------------------------------------------------------------
     */

    /**
     * ------------------------------------------------------------------------------------------------------------
     * ------------------------------Admin Side
     * ------------------------------Logs section
     */
    const [logs, setLogs] = useState([])
    useEffect(() => {
        request("GET", "/api/logs")
            .then((res) => {
                console.log("Messages response", res)
                setLogs(Array.isArray(res.data) ? res.data : []);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load user data");
                setLoading(false);
            });
    }, []);

    /**
     * ------------------------------ Completed Logs Section
     * ------------------------------------------------------------------------------------------------------------
     */

    /**
     * ------------------------------------------------------------------------------------------------------------
     * ------------------------------Admin Side
     * ------------------------------Logs section
     */
        const [benefeciaries, setBenefeciaries] = useState([])
    useEffect(() => {
        request("GET", "/api/benefeciaries")
            .then((res) => {
                console.log("Messages response", res)
                setBenefeciaries(Array.isArray(res.data) ? res.data : []);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load user data");
                setLoading(false);
            });
    }, []);

    /**
     * ------------------------------ Completed Logs Section
     * ------------------------------------------------------------------------------------------------------------
     */

    /**
     * ------------------------------------------------------------------------------------------------------------
     * ------------------------------Admin Side
     * ------------------------------Logs section
     */
    const [cities, setSitites] = useState([])
    useEffect(() => {
        request("GET", "/api/cities")
            .then((res) => {
                console.log("Messages response", res)
                setSitites(Array.isArray(res.data) ? res.data : []);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load user data");
                setLoading(false);
            });
    }, []);

    /**
     * ------------------------------ Completed Logs Section
     * ------------------------------------------------------------------------------------------------------------
     */

    if (loading) return <p>Loading users...</p>;
    if (error) return <p className="text-danger">{error}</p>;
    if (crmUsers.length === 0) return <p>No users found.</p>;

    return (
        <>
            <PageName name="Admin Panel"/>
            <div className="tabs">
                <Tabs defaultActiveKey="app-settings" id="uncontrolled-tab-example" className="mb-3">
                    <Tab eventKey="app-settings" title="App Settings">
                        <div>
                            {!config ? (
                                <NoDataMessage message="No available config"/>
                            ) : (
                                <div>
                                    <div>ID: {config.id}</div>
                                    <div>Theme: {config.theme}</div>
                                    <div>Language: {config.defaultLanguage}</div>
                                </div>
                            )}
                        </div>
                    </Tab>
                    <Tab eventKey="users" title="Users">
                        <Tabs defaultActiveKey="crmUsers" id="uncontrolled-tab-example" className="mb-3">
                            <Tab title="Users" eventKey="crmUsers">
                                {!loading && !error && crmUsers.length === 0 ? (
                                    <span className={"messageRow"}>
                                        <span>
                                            <NoDataMessage message="No users....check.."/>
                                        </span>
                                    </span>
                                ) : (
                                    <Table striped bordered hover>
                                        <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>First Name</th>
                                            <th>Last Name</th>
                                            <th>Projects Number</th>
                                            <th>Is Active</th>
                                            <th>Is Blocked</th>
                                            <th>Edit</th>
                                            <th>Delete</th>
                                        </tr>

                                        </thead>
                                        <tbody>
                                        {crmUsers.map(user => {
                                            return (
                                                <tr key={user.id}>
                                                    <th>{user.id}</th>
                                                    <th>{user.firstName}</th>
                                                    <th style={{
                                                        cursor: "pointer",
                                                        color: "blue",
                                                        textDecoration: "none"
                                                    }}
                                                        onClick={() => handleView(user.id)}>{user.login}</th>
                                                    <th>0</th>
                                                    <th>{user.isActive ? "Active" : "Not Active"}</th>
                                                    <th>{user.isBlocked ? "Blocked" : "Not Blocked"}</th>
                                                    <th>
                                                        <Button onClick={() => handleEdit(user.id)}>
                                                            Edit
                                                        </Button>
                                                    </th>
                                                    <th>
                                                        <Button variant="danger" onClick={() => handleDelete(user.id)}>
                                                            Delete
                                                        </Button>
                                                    </th>
                                                </tr>
                                            )
                                        })}
                                        </tbody>
                                    </Table>
                                )}
                            </Tab>
                            <Tab title="Customers" eventKey="customers">
                                {!loading && !error && crmUsers.length === 0 ? (
                                    <span className={"messageRow"}>
                                        <span>
                                            <NoDataMessage message="No users....check.."/>
                                        </span>

                                    </span>
                                ) : (
                                    <Table striped bordered hover>
                                        <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Nick Name</th>
                                            <th>Projects number</th>
                                            <th>Customer Logs</th>
                                            <th>Messages Number</th>
                                            <th>Edit</th>
                                            <th>Delete</th>
                                        </tr>

                                        </thead>
                                        <tbody>
                                        {customers.map(customer => {
                                            return (
                                                <tr key={customer.id}>
                                                    <th>{customer.id}</th>
                                                    <th>{customer.nickName}</th>
                                                    <th>0</th>
                                                    <th>0</th>
                                                    <th>0</th>
                                                    <th>
                                                        <Button onClick={() => handleEdit(customer.id)}>
                                                            Edit
                                                        </Button>
                                                    </th>
                                                    <th>
                                                        <Button variant="danger"
                                                                onClick={() => handleDelete(customer.id)}>
                                                            Delete
                                                        </Button>
                                                    </th>
                                                </tr>
                                            )
                                        })}
                                        </tbody>
                                    </Table>
                                )}
                            </Tab>
                        </Tabs>
                    </Tab>
                    <Tab eventKey="projects" title="Projects">
                        {!loading && !error && projects.length === 0 ? (
                            <span className={"messageRow"}>
                                        <span>
                                            <NoDataMessage message="No projects....check.."/>
                                        </span>
                                    </span>
                        ) : (
                            <Table striped bordered hover>
                                <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Project Name</th>
                                    <th>DeadLine</th>
                                    <th>Created on</th>
                                    <th>Created by</th>
                                    <th>Owner</th>
                                    <th>Status</th>
                                    <th>Delete</th>
                                </tr>

                                </thead>
                                <tbody>
                                {projects.map(project => {
                                    return (
                                        <tr key={project.id}>
                                            <th>{project.id}</th>
                                            <th style={{
                                                cursor: "pointer",
                                                color: "blue",
                                                textDecoration: "none"
                                            }}
                                                onClick={() => handleViewProj(project.id)}>{project.projectName}</th>
                                            <th>{project.deadLine}</th>
                                            <th>{project.createdOn}</th>
                                            <th>{project.creatorName}</th>
                                            <th>{project.owner}</th>
                                            <th>{project.status}</th>
                                            <th>
                                                <Button variant="danger" onClick={() => handleDeleteProj(project.id)}>
                                                    Delete
                                                </Button>
                                            </th>
                                        </tr>
                                    )
                                })}
                                </tbody>
                            </Table>
                        )}
                    </Tab>
                    <Tab eventKey="messages" title="Messages">
                        {messages.map(message => {
                            return (
                                <div>
                                    <span> - {message.author} - </span>
                                    <span>{message.messageContent} - </span>
                                    <span>{message.messageDate} - </span>
                                </div>
                            )
                        })}
                    </Tab>
                    <Tab eventKey="logs" title="Logs">
                        {logs.map(log => {
                            return (
                                <div>
                                    <span> - {log.author} - </span>
                                    <span>{log.logText} - </span>
                                    <span>{log.logDateTime} - </span>
                                    <span>{log.userNick} - </span>
                                    <span>{log.projectId} - </span>
                                </div>
                            )
                        })}
                    </Tab>

                     <Tab eventKey="Benefeciaries" title="Benefeciaries">
                        {benefeciaries.map(benefeciar => {
                            return (
                                <div key={benefeciar.id}>
                                    <span> - {benefeciar.benificiatyFirstName}  - </span>
                                    <span>{benefeciar.companyName} - </span>
                                </div>
                            )
                        })}
                    </Tab>
                    <Tab eventKey="cities" title="City">
                        {cities.map(city => {
                            return (
                                <div key={city.id}>
                                    <span>{city.city}</span>
                                </div>
                            )
                        })}
                    </Tab>
                </Tabs>
            </div>
        </>
    )
}