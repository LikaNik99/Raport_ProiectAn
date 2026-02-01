import {useState, useEffect} from "react";
import classNames from "classnames";
import {useAuth} from "../../Security/AuthContext";
import {request} from "../../Components/Helpers/AxiosHelper/AxiosHelper";
import {Button} from "react-bootstrap";
import "./LogRegForm.css"

export default function LoginRegisterForm() {
    const { login } = useAuth();
    const [active, setActive] = useState("login");
    const [form, setForm] = useState({
        firstName: "",
        lastName: "",
        login: "",
        email: "",
        password: ""
    });

    const [errorMessage, setErrorMessage] = useState(null);

    const onChangeHandler = (e) => {
        setForm({
            ...form,
            [e.target.name]: e.target.value
        });
    };

    useEffect(() => {
        if (!errorMessage) return;

        const timer = setTimeout(() => {
            setErrorMessage(null);
        }, 4000);

        return () => clearTimeout(timer);
    }, [errorMessage]);

    const getErrorMessage = (err, fallback) => {
        if (err.response?.data?.message) {
            return err.response.data.message;
        }

        if (err.response?.data?.errors) {
            return err.response.data.errors.map(e => e.msg).join(", ");
        }

        if (typeof err.response?.data === "string") {
            return err.response.data;
        }

        return fallback;
    };

    const onSubmitLogin = async (e) => {
        e.preventDefault();
        setErrorMessage(null);

        try {
            const response = await request("POST", "/auth/login", {
                login: form.login,
                password: form.password
            });

            if (!response?.data?.token) {
                throw new Error("No token in response");
            }

            login(response.data.token);
        } catch (err) {
            setErrorMessage(getErrorMessage(err, "Login failed"));
        }
    };

    const onSubmitRegister = async (e) => {
        e.preventDefault();
        setErrorMessage(null);

        try {
            const response = await request("POST", "/auth/register", form);

            if (!response?.data?.token) {
                throw new Error("No token in response");
            }

            login(response.data.token);
        } catch (err) {
            setErrorMessage(getErrorMessage(err, "Register failed"));
        }
    };
    return (
        <div className="row justify-content-center mt-5">
            <div className="col-4 mt-5 container-shadow">
                <ul className="nav nav-pills nav-justified mb-3">
                    <li className="nav-item">
                        <Button variant="outline-primary"
                                className={classNames("nav-link", active === "login" && "active")}
                                onClick={() => setActive("login")}
                        >
                            Login
                        </Button>
                    </li>
                    <li className="nav-item">
                        <Button variant="outline-primary"
                                className={classNames("nav-link ", active === "register" && "active")}
                                onClick={() => setActive("register")}
                        >
                            Register
                        </Button>
                    </li>
                </ul>
                {active === "login" && (
                    <form onSubmit={onSubmitLogin}>
                        <input name="login" placeholder="Username" className="form-control mb-2"
                               onChange={onChangeHandler}/>
                        <input name="password" type="password" placeholder="Password" className="form-control mb-2"
                               onChange={onChangeHandler}/>
                        <Button type="submit" className="btn w-100" variant="outline-primary" size="lg">Login</Button>
                    </form>
                )}
                {active === "register" && (
                    <form onSubmit={onSubmitRegister}>
                        <input name="firstName" placeholder="First name" className="form-control mb-2"
                               onChange={onChangeHandler}/>
                        <input name="lastName" placeholder="Last name" className="form-control mb-2"
                               onChange={onChangeHandler}/>
                        <input name="login" placeholder="Username" className="form-control mb-2"
                               onChange={onChangeHandler}/>
                        <input name="email" type="email" placeholder="Email" className="form-control mb-2"
                               onChange={onChangeHandler}/>
                        <input name="password" type="password" placeholder="Password" className="form-control mb-2"
                               onChange={onChangeHandler}/>

                        <div className="d-grid gap-2">
                            <Button type="submit" className="btn w-100" variant="outline-primary" size="lg">Register</Button>
                        </div>
                    </form>
                )}
                <div className="errorMessage">
                    {errorMessage && (
                        <div className="alert alert-danger mt-3">
                            {errorMessage}
                            <button
                                type="button"
                                className="btn-close"
                                onClick={() => setErrorMessage(null)}
                            />
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}