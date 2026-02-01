import { useParams } from "react-router-dom";
import { useEffect, useState } from "react";
import {request} from "../Helpers/AxiosHelper/AxiosHelper";
import PageName from "../Pagename/PageName";
import {Button, Col, FormControl, InputGroup, Row, Form} from "react-bootstrap";
export default function UserDetails() {
    const { id } = useParams();
    const [isActive, setIsActive] = useState(false);
    const [isLocked, setIsLocked] = useState(false);
    const [isAccountExp, setIsAccountExp] = useState(false);
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [formData, setFormData] = useState()
    // const [formData, setFormData] = useState({
    //     firstName: "",
    //     lastName:"",
    //     role: "",
    //     emailAddress:  "",
    //     login: "",
    //     address: "",
    //     city: "",
    //     country: "",
    //     nickName: "",
    //     phoneNumber: "",
    //     isLocked: false,
    //     isActive: true,
    //     isAccountExpired: false
    // })

    useEffect(()=>{
        if(user){
            setFormData({
                firstName: user.firstName ?? "",
            lastName: user.lastName ?? "",
            role: user.role ?? "",
            emailAddress: user.emailAddress ?? "",
            login: user.login ?? "",
            address: user.address ?? "",
            city: user.city ?? "",
            country: user.country ?? "",
            phoneNumber: user.phoneNumber ?? "",
            nickName: user.customerDTO?.nickName ?? "",
            isLocked: user.isLocked ?? false,
            isActive: user.isActive ?? true,
            isAccountExpired: user.isAccountExpired ?? false
            });
            setIsActive(user.isActive);
        setIsLocked(user.isLocked);
        setIsAccountExp(user.isAccountExpired);
        }
    }, [user])
    useEffect(() => {
        if (user) {
            setIsActive(user.isActive);
        }
    }, [user]);
    useEffect(() => {
        if (user) {
            setIsLocked(user.isLocked);
        }
    }, [user]);
    useEffect(() => {
        if (user) {
            setIsAccountExp(user.isAccountExpired);
        }
    }, [user]);

    const handleChanges = (e) => {
        const {name, value} = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }))
        console.log(e.target.value)
    };

    const handleSubmit=(e)=>{
        e.preventDefault()


        const payload = {
            firstName: formData.firstName,
            lastName: formData.lastName,
            emailAddress: formData.emailAddress,
            login: formData.login,
            address: formData.address,
            city: formData.city,
            country: formData.country,
            phoneNumber: formData.phoneNumber,
            customerDTO: {
                nickName: formData.nickName
            }
        }
        request("PUT", "/api/my/user", payload)
            .then(res => {
                setUser(res.data);
                // alert("User updated successfully");
            })
            .catch(err => {
                console.error(err);
                alert("Failed to update user");
            });
        console.log(e.target.value)
    }

    useEffect(() => {
        request("GET", `/api/users/${id}`)
            .then(res => {
                    setUser(res.data)
                    setLoading(false)
                } )
            .catch(err => {
                console.error(err);
                setError("Failed to load user");
                setLoading(false);
            });
    }, [id]);

    if (loading) {
        return <div>Loading...</div>
    }
    if (error) {
        return <div>{error}</div>
    }

    return (
        <>
            <PageName name={"User " + user.firstName + " " + user.lastName + " details"} />
            <section className="row justify-content-center mb-4">
                <Col className="container-shadow mt-4" xs={9}>
                    <form onSubmit={handleSubmit} className="row">
                        <Col sm={6}>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}><InputGroup.Text id="basic-addon1" className="text-center"> First
                                    Name </InputGroup.Text></Col>
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         placeholder={user.firstName}
                                                         area-label="firstName"
                                                         aria-describedby="basic-addon1"
                                                         disabled={true}/></Col>
                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text id="basic-addon2 mb-3"> Last Name </InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl className="sm-5 text-center"
                                                 placeholder={user.lastName}
                                                 area-label="lastName"
                                                 aria-describedby="basic-addon2"
                                                 disabled={true}/>
                                </Col>
                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text id="basic-addon3 mb-3"> Login </InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl className="sm-5 text-center"
                                                 placeholder={user.login}
                                                 area-label="lastName"
                                                 aria-describedby="basic-addon3"
                                                 disabled={true}/>
                                </Col>
                            </InputGroup>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={4}>
                                    <InputGroup.Text id="basic-addon4 mb-3"> My Role </InputGroup.Text>
                                </Col>
                                <Col sm={8}>
                                    <FormControl className="sm-5 text-center"
                                                 placeholder={user.role}
                                                 area-label="lastName"
                                                 aria-describedby="basic-addon4"
                                                 disabled={true}/>
                                </Col>
                            </InputGroup>
                        </Col>
                        <Col sm={6}>
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         placeholder="**********"
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
                            <InputGroup className="col-sm-12 mt-3">
                                <Col sm={8}><FormControl className="sm-5 text-center"
                                                         placeholder={user.emailAddress}
                                                        //  value={formData.emailAddress}
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
                                                        //  value={formData.address}
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
                                                                //  value={formData.phoneNumber}
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
                                                                 placeholder={user.customerDTO?.nickName}
                                                                //  value={formData.nickName}
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
                                                                 placeholder={user.city}
                                                                //  value={formData.city}
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
                                                                //  value={formData.country}
                                                                 placeholder={user.country}
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
                            <div>
                                <Row className="mt-4">
                                    <Col sm={4} >
                                        <InputGroup className="mb-3">
                                            <InputGroup.Checkbox
                                                checked={isActive}
                                                onChange={(e)=> {
                                                    setIsActive(e.target.checked);
                                                    console.log(e.target.value)
                                                }}
                                                aria-label="Is Active" />
                                            <InputGroup.Text>
                                                {isActive ? "User Active" : "User Not Active"}
                                            </InputGroup.Text>
                                        </InputGroup>
                                    </Col>
                                    <Col sm={4} >
                                        <InputGroup className="mb-3">
                                            <InputGroup.Checkbox
                                                checked={isLocked}
                                                onChange={(e)=> {
                                                    setIsLocked(e.target.checked);
                                                    console.log(e.target.value)
                                                }} aria-label="isAccountExpired" />
                                            <InputGroup.Text>
                                                {isLocked ? "User Blocked" : "User Not Blocked"}
                                            </InputGroup.Text>
                                        </InputGroup>
                                    </Col>
                                    <Col sm={4} >
                                        <InputGroup className="mb-3">
                                            <InputGroup.Checkbox
                                                checked={isAccountExp}
                                                onChange={(e)=> {
                                                    setIsAccountExp(e.target.checked);
                                                    console.log(e.target.value)
                                                }}  aria-label="Is Blocked" />
                                            <InputGroup.Text>
                                                {isAccountExp ? "Account Expired" : "Account up to date"}
                                            </InputGroup.Text>
                                        </InputGroup>
                                    </Col>
                                </Row>



                            </div>
                            <div className="d-grid gap-2 mt-5 justify-content-center">
                                <Button variant="outline-primary" id="button-addon1" size="lg" type="submit">
                                    Update User
                                </Button>
                            </div>
                        </Col>

                    </form>
                </Col>
            </section>
        </>
    );
}