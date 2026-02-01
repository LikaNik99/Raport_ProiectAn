namespace Core.Model.Common
{
    public enum PPResponseCode
    {
        SUCCESS = 0,
        NOT_FOUND = 1,
        DATABASE_ERROR = 2,
        INVALID_HASH = 3,
        USER_NAME_EXISTS = 4,
        INVALID_CREDENTIALS = 5,
        ALREADY_EXISTS = 6,
        FOREIGN_KEY = 7,
        AUTHOR_NOT_FOUND = 8,
        CATEGORY_NOT_FOUND = 9,
        NOT_ENOUGH_AMOUNT = 11,
        TECHNICAL_ERROR = 30,
        CRITICAL_ERROR = 31,
    }
}
