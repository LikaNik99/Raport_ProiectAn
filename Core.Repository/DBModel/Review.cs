namespace Core.Repository.Config.DBModel
{
    public class Review
    {
        public int Id { get; set; }           
        public string Recenzor { get; set; }  
        public string Mesaj { get; set; }   
        public int CarteId { get; set; }    
        public string CarteTitlu { get; set; }
    }
}
