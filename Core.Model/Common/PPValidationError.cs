namespace Core.Model.Common
{
    public struct PPValidationError
    {
        public PPResponseCode ResultCode { get; set; }
        public string ResultMessage { get; set; }
        public string ObjectName { get; set; }
    }
}
