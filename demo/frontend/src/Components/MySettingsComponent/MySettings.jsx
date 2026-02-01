import {useEffect, useState} from "react";
import "./MySettings.css"
import {request} from "../Helpers/AxiosHelper/AxiosHelper";
import PageName from "../Pagename/PageName";
import {Button, Col, FormControl, InputGroup, Row} from "react-bootstrap";

export default function MySettings(props) {
    const [user, setUser] = useState();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    useEffect(() => {
        request("GET", "/api/my/user")
            .then((res) => {
                setUser(res.data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load user data");
                setLoading(false);
            });
    }, []);

    const [formData, setFormData] = useState({
        firstName: "",
        lastName:"",
        role: "",
        emailAddress: "",
        login: "",
        address: "",
        city: "",
        country: "",
        nickName: "",
        phoneNumber: ""
    })
    const [passwordForm, setPasswordForm] = useState({
        oldPassword: "",
        newPassword: "",
        confirmPassword: ""
    });

    useEffect(()=>{
        if(user){
           setFormData({
            firstName: user.firstName || "",
            lastName:user.lastName || "",
            role: user.role || "",
                emailAddress: user.emailAddress || "",
                login: user.login || "",
                address: user.address || "",
                phoneNumber: user.phoneNumber ||"",
                city: user.city || "",
                nickName: user.nickName || "",
                country: user.country || ""
            }); 
        }
        
    }, [user])

    const handleChanges = (e) => {
        const {name, value} = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }))
    };

    const handlePassChange=e=>{
        const {name, value} = e.target;
        setPasswordForm(prev=>({...prev, [name]: value}))
    }

    const handleSubmit=(e)=>{
        e.preventDefault();
    request("PUT", "/api/my/user", {
        emailAddress: formData.emailAddress,
            address: formData.address,
            city: formData.city,
            country: formData.country,
            phoneNumber: formData.phoneNumber,
    })
        .then(res => {
            setUser(res.data);
            alert("Profile updated");
        })
        .catch(() => alert("Failed to update profile"));
    }

    const submitPassword = e => {
        e.preventDefault();

        if (passwordForm.newPassword !== passwordForm.confirmPassword) {
            alert("Passwords do not match");
            return;
        }

        request("PUT", "/api/my/user/password", passwordForm)
            .then(() => {
                alert("Password updated");
                setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
            })
            .catch(() => alert("Password change failed"));
    };

    if (loading) return <p>Loading...</p>;
    if (error) return <p>{error}</p>;
    if (!user || !user.customerDTO) return <p>No user data</p>;
    return (
        <>
            <PageName name="My Seetings"/>
            <section className="row justify-content-center mb-4">
                <Col className="container-shadow mt-4" xs={9}>
                    <form onSubmit={handleSubmit} className="row">
                        <Col sm={6}>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}><InputGroup.Text id="basic-addon1" className="text-center"> First
                                    Name </InputGroup.Text></Col>
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         value={formData.firstName}
                                                         area-label="firstName"
                                                         aria-describedby="basic-addon1"
                                                         readOnly={true}/></Col>
                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text id="basic-addon2 mb-3"> Last Name </InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl className="sm-5 text-center"
                                                 value={formData.lastName}
                                                 area-label="lastName"
                                                 aria-describedby="basic-addon2"
                                                 readOnly={true}/>
                                </Col>
                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text id="basic-addon3 mb-3"> Login </InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl className="sm-5 text-center"
                                                 value={formData.login}
                                                 area-label="lastName"
                                                 aria-describedby="basic-addon3"
                                                 readOnly={true}/>
                                </Col>
                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text id="basic-addon4 mb-3"> My Role </InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl className="sm-5 text-center"
                                                 value={formData.role}
                                                 area-label="lastName"
                                                 aria-describedby="basic-addon4"
                                                 readOnly={true}/>
                                </Col>
                            </InputGroup>
                        </Col>
                        <Col sm={6}>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         placeholder={user.emailAddress}
                                                         value={formData.emailAddress}
                                                         area-label="emailAddress"
                                                         name="emailAddress"
                                                         aria-describedby="basic-addon7"
                                                         onChange={handleChanges}
                                />
                                </Col>
                                <Col sm={4}><InputGroup.Text id="basic-addon7" className="text-center"> Email
                                    Address </InputGroup.Text></Col>

                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         placeholder={user.address}
                                                         value={formData.address}
                                                         name="address"
                                                         area-label="address"
                                                         aria-describedby="basic-addon8"
                                                         onChange={handleChanges}
                                />
                                </Col>
                                <Col sm={4}><InputGroup.Text id="basic-addon8" className="text-center"> Home
                                    Address </InputGroup.Text></Col>
                            </InputGroup>

                        </Col>
                        <Col sm={12}>
                            <Row>
                                <Col sm={6}>
                                    <InputGroup className="col-sm-12 mt-3">
                                        <Col sm={4}><InputGroup.Text id="basic-addon11" className="text-center"> Phone
                                            Number </InputGroup.Text></Col>
                                        <Col sm={8}><FormControl className="sm-5 text-center"
                                                                 placeholder={user.phoneNumber}
                                                                 value={formData.phoneNumber}
                                                                 name="phoneNumber"
                                                                 area-label="phoneNumber"
                                                                 aria-describedby="basic-addon11"
                                                                 onChange={handleChanges}/>
                                        </Col>
                                    </InputGroup>
                                    <InputGroup className="col-sm-12 mt-3">
                                        <Col sm={4}><InputGroup.Text id="basic-addon12"
                                                                     className="text-center"> Customer
                                            Name </InputGroup.Text></Col>
                                        <Col sm={8}><FormControl className="sm-5 text-center"
                                                                 placeholder={user.customerDTO.nickName}
                                                                 value={formData.nickName}
                                                                 name="nickName"
                                                                 area-label="nickName"
                                                                 aria-describedby="basic-addon12"
                                                                 onChange={handleChanges}/>
                                        </Col>
                                    </InputGroup>
                                </Col>
                                <Col sm={6}>
                                    <InputGroup className="col-sm-12 mt-3">
                                        <Col sm={8}><FormControl className="sm-5 text-center"
                                                                 value={formData.city}
                                                                 area-label="city"
                                                                 name="city"
                                                                 aria-describedby="basic-addon9"
                                                                 onChange={handleChanges}/>
                                        </Col>
                                        <Col sm={4}><InputGroup.Text id="basic-addon9"
                                                                     className="text-center"> City </InputGroup.Text></Col>
                                    </InputGroup>
                                    <InputGroup className="col-sm-12 mt-3">
                                        <Col sm={8}><FormControl className="sm-5 text-center"
                                                                 value={formData.country}
                                                                 area-label="country"
                                                                 name="country"
                                                                 aria-describedby="basic-addon10"
                                                                 onChange={handleChanges}/>
                                        </Col>
                                        <Col sm={4}><InputGroup.Text id="basic-addon10"
                                                                     className="text-center"> Country </InputGroup.Text></Col>
                                    </InputGroup>
                                </Col>
                            </Row>
                            <div className="d-grid gap-2 mt-5 justify-content-center">
                                <Button variant="outline-primary" id="button-addon1" size="lg" type="submit">
                                    Update User
                                </Button>
                            </div>
                        </Col>

                    </form>

                    <form action="sumbit">
                        <InputGroup className="col-sm-12 mt-3">
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         value={e=>setPasswordForm(e.target.value)}
                                                         type="password"
                                                         name="passowrd"
                                                         // value={formData.password}
                                                         area-label="password"
                                                         aria-describedby="basic-addon5"
                                                         onChange={handleChanges}/>
                                </Col>
                                <Col sm={4}><InputGroup.Text id="basic-addon5"
                                                             className="text-center"> Password </InputGroup.Text></Col>
                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         type="password"
                                                         placeholder="**********"
                                                         area-label="Confirm Password"
                                                         aria-describedby="basic-addon6"
                                                         onChange={handleChanges}/>
                                </Col>
                                <Col sm={4}><InputGroup.Text id="basic-addon6" className="text-center"> Confirm
                                    Password </InputGroup.Text></Col>
                            </InputGroup>
                    </form>
                </Col>
            </section>
        </>

    )
}