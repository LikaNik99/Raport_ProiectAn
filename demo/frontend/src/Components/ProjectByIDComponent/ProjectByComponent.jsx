import {useParams} from "react-router-dom";
import {useEffect, useRef, useState} from "react";
import PageName from "../Pagename/PageName";
import {request} from "../Helpers/AxiosHelper/AxiosHelper";
import {Button, Col, Form, FormControl, InputGroup, OverlayTrigger, Row, Tooltip} from "react-bootstrap";
import "./ProjectByIdComponent.css"
import { useNavigate } from "react-router-dom";
export default function ProjectByComponent() {
    const {id} = useParams();
    const [project, setProject] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);
    const [logs, setLogs] = useState([])
    const [customers, setCustomers] = useState([]);
    const [formData, setFormData] = useState({})
    const navigate = useNavigate();
    const [status, setStatus] = useState([]);
    const [messages, setMessages] = useState([]);
    const [messageText, setMessageText] = useState("");
    let cancelled = false;
    const [projectForm, setProjectForm] = useState({
        projectName: "",
        deadLine: "",
        ownerId: null,
        statusCode: "",
        budget: 0,
        cityId: null,          
        beneficiaryId: null,
    });
    const [statusList, setStatusList] = useState([]);
    const [cities, setCities] = useState([]);           // For city select if needed
    const [beneficiaries, setBeneficiaries] = useState([]);

    const [budget, setBudget] = useState(null);
    const hasFetched = useRef(false);
    const handleChanges = (e) => {
        const {name, value} = e.target;
        setProjectForm(prev => ({
            ...prev,
            [name]: value
        }))
    };
    useEffect(() => {
        if (!id || hasFetched.current) return;

        hasFetched.current = true;
        request("GET", `/api/logs/by-project/${id}`)
            .then(res => {
                if (!cancelled) {
                    setLogs(res.data);
                    setLoading(false);
                }
            })
            .catch(err => {
                if (!cancelled) {
                    console.error(err);
                    setError("Failed to load logs");
                    setLoading(false);
                }
            });
    }, [id])

    useEffect(() => {
        request("GET", "/api/customers")
            .then(res => setCustomers(res.data))
            .catch(() => setError("Failed to load customers"));

        request("GET", "/api/status")
            .then(res => setStatusList(res.data))
            .catch(() => setError("Failed to load status list"));

        // Load cities and beneficiaries if you have API for those, else omit these
        request("GET", "/api/cities")
            .then(res => setCities(res.data))
            .catch(() => setError("Failed to load cities"));

        request("GET", "/api/benefeciaries")
            .then(res => setBeneficiaries(res.data))
            .catch(() => setError("Failed to load beneficiaries"));
    }, []);

    const sendMessage = async () => {
        if (!messageText.trim()) return;
        await request("POST", `/api/project-messages/project/${id}/post-messages`, {
            messageContent: messageText
        });
        setMessageText(""); // очистить textarea
        loadMessages();
        loadLogs();
    };
    const [currentUser, setCurrentUser] = useState();
    useEffect(() => {
        request("GET", "/api/my/user")
            .then(res => {
                setCurrentUser(res.data);
                console.log(res.data)
            })
            .catch(err => {
                setError("Failed to get auth user");
            });
    }, []);
    useEffect(() => {
        request("GET", `/api/project-messages/project/${id}/get-messages`)
            .then(res => {
                setMessages(res.data)
                setLoading(false)
            })
            .catch(err => {
                setError("Failed to load Project");
                setLoading(false);
            });
    }, [id])
    const loadMessages = async () => {
        const res = await request("GET", `/api/project-messages/project/${id}/get-messages`);
        setMessages(res.data);
    };
    const loadLogs = async () => {
        const res = await request("GET", `/api/logs/by-project/${id}`);
        setLogs(res.data);
    };
    useEffect(() => {
        loadMessages();
        loadLogs();
    }, [id]);
    const handleSubmit = (e) => {
        e.preventDefault()
        const payload = {
            projectName: formData.projectName,
            deadLine: formData.deadLine,
            owner: formData.owner,
            status: formData.status,

        }
    }
        const handleUpdateProject = async (projectId) => {
              try{
                  await request("PUT", `/api/project-update/${projectId}`, projectForm)
                  navigate("/dashboard")
              }
              catch(err){
                  console.log(err)
              }
        };



    
    const userRole = currentUser?.role;
    console.log(currentUser)
    console.log(userRole)

    const canEditBudget = userRole === "ADMIN" || userRole === "MANAGER";
    const canEditProject = canEditBudget;
    useEffect(() => {
        request("GET", `/api/project/${id}`)
            .then(res => {
                setProject(res.data)
                setLoading(false)
                console.log("this is the project got by ID")
                console.log(res.data)
            })
            .catch(err => {
                console.error(err);
                setError("Failed to load Project");
                setLoading(false);
            });
    }, [id]);
    useEffect(()=>{
        if(!project) return;
        setProjectForm({
        projectName: project.projectName ?? "",
        deadLine: project.deadLine ??"",
        ownerId: project.ownerId ?? null,
        statusCode: project.statusName ?? "",
        budget: project.budget ?? 0,
        cityId: project.cityId ?? null,  
        beneficiaryId: project.beneficiaryId ?? null,
        })
    }, [project])
    if (error) return <p>{error}</p>;
    if (!project) return <p>Loading...</p>;
    return (
        <div>

            <PageName name={"Project " + project.projectName + " details"}/>
            <section className="row justify-content-center mb-4">
                <Col className="container-shadow mt-4" xs={9}>
                    <form onSubmit={(e)=>{
                        e.preventDefault();
                        handleUpdateProject(id)
                    }} className="row">
                        <Col sm={6}>
                            {/* Project Name */}
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text className="text-center">Project Name</InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl
                                    className="text-center"
                                        name="projectName"
                                        value={projectForm.projectName}
                                        onChange={handleChanges}
                                        readOnly={!canEditProject}
                                        style={{
                                            cursor: canEditProject ? "text" : "not-allowed",
                                            backgroundColor: canEditProject ? "white" : "#f8f9fa",
                                        }}
                                    />
                                </Col>
                            </InputGroup>

                            {/* Project DeadLine */}
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text>Project DeadLine</InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <Form.Control
                                        type="date"
                                        name="deadLine"
                                        value={projectForm.deadLine}
                                        onChange={handleChanges}
                                        readOnly={!canEditProject}
                                        style={{
                                            cursor: canEditProject ? "text" : "not-allowed",
                                            backgroundColor: canEditProject ? "white" : "#f8f9fa",
                                            textAlign: "center",
                                        }}
                                    />
                                </Col>
                            </InputGroup>

                            {/* Project Owner */}
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text>Project Owner</InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <Form.Select
                                        name="ownerId"
                                        value={projectForm.ownerId}
                                        onChange={canEditProject ? handleChanges : undefined}
                                        style={{
                                            pointerEvents: canEditProject ? "auto" : "none",
                                            backgroundColor: canEditProject ? "white" : "#f8f9fa",
                                        }}
                                    >
                                        <option value={project.ownerId}>{project.ownerName}</option>
                                        {customers.map(customer => (
                                            <option key={customer.id} value={customer.id}>
                                                {customer.nickName}
                                            </option>
                                        ))}
                                    </Form.Select>
                                </Col>
                            </InputGroup>

                            {/* Project Status */}
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text>Project Status</InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <Form.Select
                                        name="statusCode"
                                        value={projectForm.statusCode}
                                        onChange={handleChanges}
                                        className="text-center"
                                    >
                                        <option value="">{project.statusName}</option>
                                        {statusList.map(status => (
                                            <option key={status.id} value={status.code}>
                                                {status.displayName}
                                            </option>
                                        ))}
                                    </Form.Select>
                                </Col>
                            </InputGroup>

                            {/* City (read-only) */}
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text>City</InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    {/* Assuming cities list available, but field is read-only */}
                                    <Form.Select
                                        name="cityId"
                                        value={projectForm.cityId}
                                        onChange={canEditProject ? handleChanges : undefined}
                                        style={{
                                            pointerEvents: canEditProject ? "auto" : "none",
                                            backgroundColor: canEditProject ? "white" : "#f8f9fa",
                                        }}
                                    >
                                        {cities.length > 0 ? (
                                            cities.map(city => (
                                                <option key={city.cityId} value={city.cityId}>
                                                    {city.city}
                                                </option>
                                            ))
                                        ) : (
                                            <option>{project.city?.name || "No city"}</option>
                                        )}
                                    </Form.Select>
                                </Col>
                            </InputGroup>

                            {/* Beneficiary (read-only) */}
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text>Beneficiary</InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    {/* Beneficiary select disabled as per backend */}
                                    <Form.Select
                                        name="beneficiaryId"
                                        value={projectForm.beneficiaryId}
                                        onChange={canEditProject ? handleChanges : undefined}
                                        style={{
                                            pointerEvents: canEditProject ? "auto" : "none",
                                            backgroundColor: canEditProject ? "white" : "#f8f9fa",
                                        }}
                                    >
                                        {beneficiaries.length > 0 ? (
                                            beneficiaries.map(b => (
                                                <option key={b.id} value={b.id}>
                                                    {b.companyName || b.nickName}
                                                </option>
                                            ))
                                        ) : (
                                            <option>{project.companyName || "No beneficiary"}</option>
                                        )}
                                    </Form.Select>
                                </Col>
                            </InputGroup>

                            {/* Budget */}
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text>Project Budget</InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl
                                        name="budget"
                                        type="number"
                                        value={projectForm.budget}
                                        onChange={
                                            canEditBudget
                                            ? (e) =>
                                                setProjectForm((prev) => ({
                                                    ...prev,
                                                    budget: Number(e.target.value),
                                                }))
                                            : undefined
                                        }
                                        readOnly={!canEditBudget}
                                        style={{
                                            cursor: canEditBudget ? "text" : "not-allowed",
                                            backgroundColor: canEditBudget ? "white" : "#f8f9fa",
                                            textAlign: "center",
                                        }}
                                    />
                                </Col>
                            </InputGroup>
                        </Col>

                        <Col sm={6}>
                            {/* Logs section */}
                            <div className="mt-3 shadow logsBlock" style={{ height: "400px", overflowY: "scroll" }}>
                                {logs.map(log => (
                                    <div className="shadow m-2 p-2" key={log.id}>
                                        <span>{log.logDateTime}</span> <span>{log.logText}</span>
                                    </div>
                                ))}
                            </div>
                        </Col>

                        <Col sm={12} className="d-grid gap-2 mt-5 justify-content-center">
                            <Button variant="outline-primary" size="lg" type="submit">
                                Update Project
                            </Button>
                        </Col>
                    </form>
                </Col>

                <div className="row justify-content-center mb-5 mt-4 messageSection">
                    <Col xs={8}>
                        <div className="messagesBlock shadow">
                            {Array.isArray(messages) ? (
                                messages.map((message, index) => (
                                    <div
                                        key={message.id}
                                        className={`messageItem shadow ${index % 2 === 0 ? "left" : "right"}`}
                                    >
                                        <div className="messageText">{message.messageContent}</div>

                                        <div className="dateAuthor">
                                            <div className="messageAuthor">{message.author}</div>
                                            <div className="messageTime">{message.messageDate}</div>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="error">no messages</div>
                            )}
                        </div>
                    </Col>
                    {/* <Row className="align-items-end"> */}
                    <Col xs={8}>
                        <div className="row justify-content-center mb-5 mt-4 messageSection">
                            <Col xs={9}>
                                <Form.Control
                                    as="textarea"
                                    rows={3}
                                    className="no-resize shadow"
                                    value={messageText}
                                    onChange={(e) => setMessageText(e.target.value)}
                                    onKeyDown={(e) => {
                                        if (e.key === "Enter" && !e.shiftKey) {
                                            e.preventDefault();
                                            sendMessage();
                                        }
                                    }}
                                />
                            </Col>

                            <Col xs={3} className="d-grid">
                                <Button
                                    variant="outline-primary"
                                    size="lg"
                                    readOnly={!messageText.trim()}
                                    onClick={sendMessage}
                                >
                                    Send
                                </Button>
                            </Col>
                        </div>
                    </Col>
                    {/* </Row> */}
                </div>
            </section>
        </div>

    );
}