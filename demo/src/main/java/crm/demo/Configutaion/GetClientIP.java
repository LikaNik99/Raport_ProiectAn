package crm.demo.Configutaion;

import jakarta.servlet.http.HttpServletRequest;

public class GetClientIP {
    public static String getClientIP(HttpServletRequest request) {
        String headers = request.getHeader("X-Forwarded-For");

        if(headers !=null && !headers.isEmpty() ){
            return headers.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
