Instructiune de pornire a aplicatiei 

merg in directoria unde este docker yaml care descrie containerul cu baza de date si il ridic 

cd  C:\\Users\\VasileArtenii\\Documents\\CrmDemoProject\\CRM-Demo-Project\\temp\\Composes

docker compose up -d



merg in directoria cu frontend si startam 



&nbsp;cd C:\\Users\\VasileArtenii\\Documents\\CrmDemoProject\\CRM-Demo-Project\\demo\\frontend

npm install

npm start





Merg in frontend 

cd C:\\Users\\VasileArtenii\\Documents\\JavaProject\\CRM-Demo-Project-main\\demo\\frontend



.\\launch.cmd



Aplicatia porneste 3 ferestre si deschide fronendul mysql logurile si serverul spring boot 

configurez Configure Frontend API targe



baseURL: "http://172.20.130.34:8080"



axios.defaults.baseURL = "http://172.20.130.34:8080";





4\. Start Backend (Server)



cd C:\\Users\\VasileArtenii\\Documents\\CrmDemoProject\\CRM-Demo-Project\\demo

mvn spring-boot:run -DskipTests





si este available 

http://172.20.130.34:8080





5\. CORS Configuration (Backend)

configuration.setAllowedOrigins(List.of(

&nbsp;   "http://localhost:3000",

&nbsp;   "http://172.20.130.34:3000"

));

configuration.setAllowedHeaders(List.of("\*"));

configuration.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));

configuration.setAllowCredentials(true);

