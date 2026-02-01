namespace Core.Model.Common
{
    public class PPResult
    {
        public long TransactionId { get; set; }
        public PPResponseCode ResultCode { get; set; }
        public string? ResultMessage { get; set; }
        public string? ResultPrivateMessage { get; set; }
        public IEnumerable<PPValidationError>? ValidationErrors { get; set; }

        public bool IsOK { get { return ResultCode == PPResponseCode.SUCCESS && (ValidationErrors == null || !ValidationErrors.Any()); } }
    }

    public class PPResult<T> : PPResult
    {
        public T? ReturnObject { get; set; }
    }

}
