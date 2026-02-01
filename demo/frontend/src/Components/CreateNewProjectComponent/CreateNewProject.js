import {useEffect, useState} from "react";
import PageName from "../Pagename/PageName";
import "./CreateNewProject.css"
import {useNavigate} from "react-router-dom";
import {request} from "../Helpers/AxiosHelper/AxiosHelper";


export default function CreateNewProject(props) {

    const [customers, setCustomers] = useState([]);
    const navigate = useNavigate();
    const [benefeciaries, setBenefeciaries] = useState([]);
    const [cities, setCities] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    // RANGE VALUE
    const handleBudgetChange = (event)=>{
        const newValue  = Number(event.target.value);
        if (!isNaN(newValue)) {
            setFormData(prev => ({
                ...prev,
                budget: newValue
            }));
        }
    }

    useEffect(() => {
        request("GET", "/api/customers")
            .then((res) => {
                setCustomers(res.data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load setBenefeciaries data");
                setLoading(false);
            });
    }, []);


    useEffect(() => {
        request("GET", "/api/benefeciaries")
            .then((res) => {
                setBenefeciaries(res.data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load Beneficiaries data");
                setLoading(false);
            });
    }, []);

    useEffect(() => {
        request("GET", "/api/cities")
            .then((res) => {
                setCities(res.data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError("Failed to load Cities data");
                setLoading(false);
            });
    }, []);


    const [formData, setFormData] = useState({
        projectName: "",
        deadLine: "",
        projectDescription: "",
        ownerId: "",
        cityId: "",
        beneficiaryId: "",
        budget: 100,
        statusCode: "100"
    })

    const handleChanges = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };
        const handleSubmit = async () => {
            try{
                await request("POST", `/api/project-add`, formData)
                navigate("/dashboard")
            }
            catch(err){
                console.log(err)
            }
        };

    return (
        <div>

            <div>
                <PageName name="Create new Project"/>
            </div>
            <div>
                <form onSubmit={handleSubmit}>
                    <div className="formContainer">
                        <div className="labelsInputsBlock inputsFirstRow">
                            <div className="labelsInputs">
                                <label htmlFor="projectName"> Project Name</label>
                                <input id="projectName" required name="projectName" value={formData.projectName}
                                       type="text" onChange={handleChanges}/>
                            </div>

                            <div className="labelsInputs">
                                <label htmlFor="deadLine">Dead Line</label>
                                <input id="deadLine" required name="deadLine" value={formData.deadLine}
                                       onChange={handleChanges} type="date"/>
                            </div>
                            <div className="labelsInputs">
                                <label htmlFor="customerId">Assign to </label>
                                <select title="assignTo" required id="customerId" name="ownerId"
                                        value={formData.ownerId} onChange={handleChanges}>
                                    <option value=""> select name</option>
                                    {
                                        customers.map(customer => {
                                            return (<option key={customer.id} value={customer.id}>
                                                {customer.nickName}
                                            </option>)
                                        })
                                    }
                                </select>
                            </div>
                        </div>
                        <div className="formContainer">
                            <div className="labelsInputsBlock inputsFirstRow">
                                <div className="labelsInputs">
                                    <label htmlFor="city">City </label>
                                    <select
                                        id="city"
                                        name="cityId"
                                        required
                                        value={formData.cityId}
                                        onChange={handleChanges}
                                    >
                                        <option value="">Select city</option>
                                        {cities.map(city => (
                                            <option key={city.id} value={city.id}>
                                                {city.city}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="labelsInputs">
                                    <label htmlFor="Beneficiary">Beneficiary </label>
                                    <select
                                        id="beneficiary"
                                        name="beneficiaryId"
                                        required
                                        value={formData.beneficiaryId}
                                        onChange={handleChanges}
                                    >
                                        <option value="">Select Beneficiary</option>
                                        {benefeciaries.map(benef => (
                                            <option key={benef.id} value={benef.id}>
                                                {benef.companyName}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                <div className="labelsInputs">
                                    <div className="labelsInputs">
                                        <label htmlFor="budget">Budget</label>

                                        <input
                                            type="range"
                                            id="budget"
                                            min="100"
                                            max="1000000"
                                            step="50"
                                            value={formData.budget}
                                            onChange={handleBudgetChange}
                                        />

                                        <input
                                            type="number"
                                            min="100"
                                            max="1000000"
                                            step="50"
                                            value={formData.budget}
                                            onChange={handleBudgetChange}
                                        />
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="formContainer">
                        <div className=" inputsSecondRow">
                            <div className="labelsInputs">
                                <label htmlFor="projectDescription">Description</label>
                                <textarea name="projectDescription" required value={formData.projectDescription}
                                          onChange={handleChanges} id="projectDescription"/>
                            </div>
                        </div>
                    </div>
                    <div className="formContainer">
                        <div className=" inputsSecondRow">
                            <div className="labelsInputs">
                                <button className="btn" type="submit"> Create New Project</button>
                            </div>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    )
}