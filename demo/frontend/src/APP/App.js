import Header from "../Components/Header/Header.Header";
import '.././index.css'
import Home from "../Components/HomeComponent/Home";
import Dashboard from "../Components/DashboardComponent/Dashboard";
import {Routes, Route, Navigate} from 'react-router-dom'
import MyProjects from "../Components/MyProjectsComponent/MyProjects";
import MySettings from "../Components/MySettingsComponent/MySettings";
import CreateNewProject from "../Components/CreateNewProjectComponent/CreateNewProject";
import LoginRegisterForm from "../APP/LoginAndRegister/LoginRegisterForm";
import ProtectedRoute from "../Security/ProtectedRoute";
import {useAuth} from "../Security/AuthContext";
import RoleRoute from "../Security/RoleRoute";
import AdminPanel from "./../Components/AdminPanelComponent/AdminPanel"
import {useEffect} from "react";
import {setupAxiosInterceptors} from "../Components/Helpers/AxiosHelper/AxiosInterceptors";
import NotFound from "../Components/NotFoundComponent/NotFound";
import UserDetails from "../Components/UserDetailsComponent/UserDetails";
import ProjectByComponent from "../Components/ProjectByIDComponent/ProjectByComponent"
import ServerDownModal from "../Components/GlobalModal/ServerDownModal";
import TestRequest from "../Components/ZTESTBack/TestRequest";
import api from "../Components/Helpers/AxiosHelper/Axios";
function App() {

    useEffect(()=>{
        api.get("/actuator/health").catch(()=>{

        });
    }, [])

    const callBackend = async()=>{
        try{
            await api.get("/actuator/health");
        }catch(e){

        }
    }

    const {logout} = useAuth();
    useEffect(() => {
        setupAxiosInterceptors(logout);
    }, [logout]);
    return (
        <div className="App">
            <Header pageTitle="Front End Authentication with JWT"/>
            <ServerDownModal />
            {/* <TestRequest /> */}
            <div className="container-fluid">
                <Routes>
                    <Route path="/login-register" element={<PublicOnly />}/>
                    <Route path="/" element={<Home/>}/>

                    <Route element={<ProtectedRoute/>}>
                        <Route path="/dashboard" element={<Dashboard/>}/>
                        <Route path="/my-projects" element={<MyProjects/>}/>
                        <Route path="/my-settings" element={<MySettings/>}/>
                        <Route path="/create-new-project" element={<CreateNewProject/>}/>
                        <Route path="/project/:id" element={<ProjectByComponent/>}/>

                        <Route element={<RoleRoute allowedRoles={["ADMIN"]}/>}>
                            <Route path="/admin" element={<AdminPanel/>}/>
                            <Route path="/users/:id" element={<UserDetails/>}/>
                        </Route>

                        <Route path="*" element={<NotFound/>}/>
                    </Route>
                </Routes>
            </div>
        </div>
    );
}

function PublicOnly() {
    const { token } = useAuth();
    return token ? <Navigate to="/" replace /> : <LoginRegisterForm />;
}

export default App;