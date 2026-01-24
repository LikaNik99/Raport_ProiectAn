using HrWebRecruitment.Services;
using Microsoft.AspNetCore.Mvc;
using Minio;
using Minio.DataModel;
using MongoDB.Bson;
using Newtonsoft.Json;

namespace HrWebRecruitment.Controllers
{
    public class VacanciesController(DbService dbService, MinioService minioService) : Controller
    {
        public async Task<string> Get()
        {
            return JsonConvert.SerializeObject(await dbService.GetVacancies());
        }

        [HttpPost("aplicate")]
        public async Task<IActionResult> Aplicate(IFormCollection form)
        {
            try
            {
                var file = Request.Form.Files.FirstOrDefault();
                if (file == null)
                    return BadRequest("No file uploaded.");

                string linkToCv = await minioService.UploadCV(file);

                await dbService.ApplyAsync(new Candidat()
                {
                    Id = ObjectId.GenerateNewId(),
                    FirstName = Request.Form["Name"],
                    LastName = Request.Form["Surname"],
                    Email = Request.Form["Email"],
                    PhoneNumber = Request.Form["Phone"],
                    VacancyName = Request.Form["VacancyTitle"],
                    LinkToCv = linkToCv
                });

                return Ok(new { message = "CV uploaded successfully", link = linkToCv });
            }
            catch (Exception ex)
            {
                // Log error appropriately
                return StatusCode(500, "Internal server error");
            }
        }
    }
}
