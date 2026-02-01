package crm.demo.Helpers;

public class Helper {
    public static void main(String[] args) {
        String token1 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJxd2UiLCJpYXQiOjE3MTczMjI4NDgsImV4cCI6MTcyMDkyMjg0OH0.Kue9qeVyZAsNCIFrGj8WRYKmZejN_tlPmrTkLtajOX4";
        String token2 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJxd2UiLCJpYXQiOjE3MTczMjI4NDgsImV4cCI6MTcyMDkyMjg0OH0.Kue9qeVyZAsNCIFrGj8WRYKmZejN_tlPmrTkLtajOX4";

        boolean isEqual = token1.matches(token2);
        System.out.println(isEqual);
    }
}