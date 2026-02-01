import ReactDOM from 'react-dom/client';
import './index.css';
import App from './APP/App';
import reportWebVitals from './reportWebVitals';
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from './Security/AuthContext';
import 'bootstrap/dist/css/bootstrap.min.css'
import 'bootstrap/dist/css/bootstrap.css';
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <AuthProvider>
        <BrowserRouter>
            <App />
         </BrowserRouter>
    </AuthProvider>
);
reportWebVitals();