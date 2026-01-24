using HrWebRecruitment.Services;
using Microsoft.AspNetCore.Mvc;
using Newtonsoft.Json;

namespace HrWebRecruitment.Controllers
{
    public class UserController(DbService dbService) : Controller
    {
        public async Task<string> Login(string uname, string pname)
        {
            var user = await dbService.GetUser(uname, pname);
            string json = JsonConvert.SerializeObject(user);
            return json;

        }
        //app.MapPost("/login", (HttpContext context, ModelContext db) =>
        //{
        //    try
        //    {
        //        var form = context.Request.Form;
        //        var user = db.Users.ToList().FirstOrDefault(user => user.Username == form["uname"] && user.Password == form["pname"]);
        //        string json = JsonConvert.SerializeObject(user);
        //        return (json);
        //    }
        //    catch (Exception ex)
        //    {
        //        logger.LogError(ex.Message + " " + ex.StackTrace);
        //        return ex.Message;
        //    }
        //});
    }
}
