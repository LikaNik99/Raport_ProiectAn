namespace HrWebRecruitment.Models.Config
{
    public class MinioConfigModel
    {
        public required string Endpoint { get; set; }
        public required string AccessKey { get; set; }
        public required string SecretKey { get; set; }
        public required bool UseSSL { get; set; } = false;
    }
}