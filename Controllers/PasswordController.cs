using Amazon.Runtime.Internal;
using Microsoft.AspNetCore.Mvc;
using PasswordManager.Models;
using PasswordManager.Repositories;
using System.Text.Json;

namespace PasswordManager.Controllers
{
    [ApiController]
    [Route("[controller]")]
    public class PasswordController(DbService dbService) : Controller
    {
        public IActionResult Index()
        {
            return View();
        }

        // GET /passwords?userId=xxx
        [HttpGet("/passwords")]
        public async Task<IActionResult> GetPasswordsAsync([FromQuery] string? userId = null)
        {
            try
            {
                var passwordsJson = await dbService.GetPasswordsAsync(userId);
                return Ok(passwordsJson);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Server error", details = ex.Message });
            }
        }

        // POST /passwords
        [HttpPost("/passwords")]
        public async Task<IActionResult> AddPasswordAsync(Dictionary<string, object> passwordData)
        {
            try
            {
                var pass = MapToPasswordModel(passwordData);

                var resultJson = await dbService.AddPasswordAsync(pass);
                return Ok(resultJson);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Ssrver error", details = ex.Message });
            }
        }

        // PUT /passwords/{id}
        [HttpPut("/passwords/{id}")]
        public async Task<IActionResult> UpdatePasswordAsync( string id, Dictionary<string, object> updates)
        {
            try
            {
                var updatedPassword = MapToPasswordModel(updates);
                var resultJson = await dbService.UpdatePasswordAsync(id, updatedPassword);
                return Ok(resultJson);
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Server error", details = ex.Message });
            }
        }

        // DELETE /passwords/{id}
        [HttpDelete("/passwords/{id}")]
        public async Task<IActionResult> DeletePasswordByIdAsync( string id)
        {
            try
            {
                var resultJson = await dbService.DeletePasswordByIdAsync(id);
                return Content(resultJson, "application/json");
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Server error", details = ex.Message });
            }
        }

        // GET /requests?userId=xxx&status=yyy
        [HttpGet("/requests")]
        public async Task<IActionResult> GetRequestsAsync([FromQuery] string? userId = null, [FromQuery] string? status = null)
        {
            try
            {
                var resultJson = await dbService.GetRequestsAsync(userId, status);
                return Content(resultJson, "application/json");
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Server error", details = ex.Message });
            }
        }

        // POST /requests
        [HttpPost("/requests")]
        public async Task<IActionResult> AddRequestAsync([FromBody] Dictionary<string, object> requestData)
        {
            try
            {
                var requestModel = MapToRequestModel(requestData);

                var resultJson = await dbService.AddRequestAsync(requestModel);
                return Content(resultJson, "application/json");
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Server error", details = ex.Message });
            }
        }

        // PUT /requests/{id}
        [HttpPut("/requests/{id}")]
        public async Task<IActionResult> UpdateRequestAsync([FromRoute] string id, [FromBody] Dictionary<string, object> updates)
        {
            try
            {
                var requestModel = MapToRequestModel(updates);

                var resultJson = await dbService.UpdateRequestAsync(id, requestModel);
                return Content(resultJson, "application/json");
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Server error", details = ex.Message });
            }
        }

        // DELETE /requests/{id}
        [HttpDelete("/requests/{id}")]
        public async Task<IActionResult> DeleteRequestByIdAsync(string id)
        {
            try
            {
                var resultJson = await dbService.DeleteRequestByIdAsync(id);
                return Content(resultJson, "application/json");
            }
            catch (Exception ex)
            {
                return StatusCode(500, new { error = "Server error", details = ex.Message });
            }
        }

        // Helper method to map Dictionary<string, object> to RequestModel
        private RequestModel MapToRequestModel(Dictionary<string, object> requestData)
        {
            var model = new RequestModel();

            if ((requestData.TryGetValue("updates", out var dictionaryObj) || requestData.TryGetValue("requestData", out  dictionaryObj) || requestData.TryGetValue("dictionary", out dictionaryObj)) && dictionaryObj is System.Text.Json.JsonElement jsonElement && jsonElement.ValueKind == System.Text.Json.JsonValueKind.Object)
            {
                var dict = jsonElement;

                if (dict.TryGetProperty("nume", out var numeProp))
                    model.Nume = numeProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("scop", out var scopProp))
                    model.Scop = scopProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("detalii", out var detaliiProp))
                    model.Detalii = detaliiProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("prioritate", out var prioritateProp))
                    model.Prioritate = prioritateProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("data", out var dataProp))
                {
                    if (dataProp.ValueKind == JsonValueKind.Object &&
                        dataProp.TryGetProperty("$date", out var dateProp))
                    {
                        if (dateProp.TryGetDateTime(out var parsedDate))
                        {
                            model.Data = parsedDate;
                        }
                        else if (long.TryParse(dateProp.GetString(), out var timestamp))
                        {
                            model.Data = DateTimeOffset.FromUnixTimeMilliseconds(timestamp).UtcDateTime;
                        }
                        else
                        {
                            model.Data = DateTime.UtcNow;
                        }
                    }
                    else if (DateTime.TryParse(dataProp.GetString(), out var parsedDate))
                    {
                        model.Data = parsedDate;
                    }
                    else
                    {
                        model.Data = DateTime.UtcNow;
                    }
                }
                else
                {
                    model.Data = DateTime.UtcNow;
                }
                if (dict.TryGetProperty("clientId", out var clientIdProp))
                    model.ClientId = clientIdProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("status", out var statusProp))
                    model.Status = statusProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("id", out var idProp))
                    model.Id = idProp.GetString();
            }
            else
            {
                // fallback to old logic if "dictionary" is not present or not an object
                if (requestData.TryGetValue("nume", out var nume))
                    model.Nume = nume?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("scop", out var scop))
                    model.Scop = scop?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("detalii", out var detalii))
                    model.Detalii = detalii?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("prioritate", out var prioritate))
                    model.Prioritate = prioritate?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("data", out var data))
                {
                    if (DateTime.TryParse(data?.ToString(), out var parsedDate))
                        model.Data = parsedDate;
                    else
                        model.Data = DateTime.UtcNow;
                }
                else
                {
                    model.Data = DateTime.UtcNow;
                }
                if (requestData.TryGetValue("clientId", out var clientId))
                    model.ClientId = clientId?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("status", out var status))
                    model.Status = status?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("id", out var id))
                    model.Id = id?.ToString();
            }
            return model;
        }

        private PasswordModel MapToPasswordModel(Dictionary<string, object> requestData)
        {
            var model = new PasswordModel();

            // Set mandatory creation date and initial history
            model.CreatedDate = DateTime.UtcNow;
            model.LastEdited = DateTime.UtcNow;
            model.Istoric.Add(new HistoryEntry { Actiune = "creat", Data = model.CreatedDate });

            // --- Logic to handle nested JSON data (similar to your RequestModel logic) ---
            if ((requestData.TryGetValue("updates", out var dictionaryObj) || requestData.TryGetValue("passwordData", out dictionaryObj)) &&
                dictionaryObj is JsonElement jsonElement &&
                jsonElement.ValueKind == JsonValueKind.Object)
            {
                var dict = jsonElement;

                // Map Password Fields from nested dictionary
                if (dict.TryGetProperty("userId", out var userIdProp))
                    model.UserId = userIdProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("denumire", out var denumireProp))
                    model.Denumire = denumireProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("proprietar", out var proprietarProp))
                    model.Proprietar = proprietarProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("tip", out var tipProp))
                    model.Tip = tipProp.GetString() ?? "Conectare";
                if (dict.TryGetProperty("username", out var userProp))
                    model.Username = userProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("parola", out var parolaProp))
                    model.Parola = parolaProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("saitweb", out var siteProp))
                    model.SaitWeb = siteProp.GetString() ?? string.Empty;
                if (dict.TryGetProperty("detalii", out var detaliiProp))
                    model.Detalii = detaliiProp.GetString() ?? string.Empty;
            }
            // --- Fallback to direct Dictionary access ---
            else
            {
                // Map Password Fields from top-level dictionary
                if (requestData.TryGetValue("userId", out var userId))
                    model.UserId = userId?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("denumire", out var denumire))
                    model.Denumire = denumire?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("proprietar", out var proprietar))
                    model.Proprietar = proprietar?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("tip", out var tip))
                    model.Tip = tip?.ToString() ?? "Conectare";
                if (requestData.TryGetValue("username", out var user))
                    model.Username = user?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("parola", out var parola))
                    model.Parola = parola?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("saitweb", out var site))
                    model.SaitWeb = site?.ToString() ?? string.Empty;
                if (requestData.TryGetValue("detalii", out var detalii))
                    model.Detalii = detalii?.ToString() ?? string.Empty;
            }

            // Ensure all fields are initialized (optional check)
            if (string.IsNullOrEmpty(model.Tip)) model.Tip = "Conectare";

            return model;
        }
    }
}
