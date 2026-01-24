using HrWebRecruitment.Services;
using Microsoft.AspNetCore.Mvc;
using MongoDB.Bson;
using Newtonsoft.Json;

namespace HrWebRecruitment.Controllers
{
    [Route("[controller]")]
    public class AdminController(DbService dbService, ILogger<AdminController> logger) : Controller
    {
        public IActionResult Index()
        {
            return View();
        }

        [HttpGet("GetVacancy")]
        public async Task<string> GetVacancy()
        {
            return JsonConvert.SerializeObject(await dbService.GetVacancies());
        }

        [HttpGet("GetHiring")]
        public async Task<string> GetHiring()
        {
            return await dbService.GetHirings();
        }

        [HttpGet("GetUsers")]
        public async Task<string> GetUsers()
        {
            var result = await dbService.GetUsers();
            return result;
        }

        [HttpGet("GetEmployees")]
        public async Task<string> GetEmployees()
        {
            var result = await dbService.GetEmployees();
            return result;
        }

        [HttpGet("GetDictionary")]
        public async Task<string> GetDictionary()
        {
            var result = await dbService.GetDictionary();
            return result.ToJson();
        }

        [HttpGet("GetStatuses")]
        public async Task<string> GetStatuses()
        {
            return await dbService.GetStatuses();
        }
        [HttpGet("GetCandidats")]
        public async Task<string> GetCandidats()
        {
            var result = await dbService.GetCandidats();
            return result.ToJson();
        }


        [HttpPost("{target}/{actionName}")]
        public async Task<IActionResult> HandleAction(string target, string actionName)
        {
            var form = Request.Form;
            string _json = string.Empty;
            try
            {
                switch (actionName)
                {
                    case "getCandidats":
                        var candidats = await dbService.GetCandidats();
                        _json = JsonConvert.SerializeObject(candidats);
                        return Content(_json);
                    case "getSelectedCandidat":
                        var selectedCandidat = (await dbService.GetCandidats())
                            .FirstOrDefault(c => c.Id == form["ID"]);
                        _json = JsonConvert.SerializeObject(selectedCandidat);
                        return Content(_json);
                    case "SetNewUser":
                        var dictionaries = await dbService.GetDictionary();
                        var hrRole = dictionaries.First(dictionary => dictionary.Name == "HR").Id;
                        var mail = form["FirstName"].ToString().ToLowerInvariant().Substring(0, 1) + form["LastName"].ToString().ToLower() + "@email.em";
                        var newUser = new User
                        {
                            UserName = form["Username"],
                            Password = form["Password"],
                            FirstName = form["FirstName"],
                            LastName = form["LastName"],
                            Email = mail,
                            RoleId = hrRole.ToString(),
                            StartDate = DateTime.Now,
                            EndDate = null
                        };
                        await dbService.AddUserAsync(newUser);
                        return Ok();
                    case "getVacancyV":
                        var vacancies = await dbService.GetVacancies();
                        var vacancy = vacancies.FirstOrDefault(v => v.Title == form["Title"]);
                        _json = JsonConvert.SerializeObject(vacancy);
                        return Content(_json);
                    case "SetNewVacancy":
                        var newVacancy = new Vacancy
                        {
                            Title = form["Title"],
                            Description = form["Description"],
                            StartDate = DateTime.Now,
                            EndDate = DateTime.Now.AddMonths(1)
                        };
                        await dbService.AddVacancyAsync(newVacancy);
                        return Content("Success");
                    case "SetNewDictionary":
                        var dictName = form["Name"];
                        var dictType = form["Type"];
                        var dictDescription = form["Description"];
                        var newDictionary = new Dictionary
                        {
                            Name = dictName,
                            Type = dictType,
                            Description = dictDescription
                        };
                        await dbService.AddDictionaryAsync(newDictionary);
                        return Ok();
                    case "EditVacancy":
                        var editVacancies = await dbService.GetVacancies();
                        var editVacancy = editVacancies.FirstOrDefault(v => v.Id.ToString() == form["ID"]);
                        if (editVacancy != null)
                        {
                            editVacancy.Title = form["Title"];
                            editVacancy.Description = form["Description"];
                            await dbService.UpdateVacancyAsync(editVacancy);
                        }
                        return Content("Success");
                    case "EditHiring":
                        var status = form["Status"];
                        var candidatId = form["CandidatID"];
                        var id = form["ID"];
                        if (status == "Hired")
                        {
                            var hirings = await dbService.GetHirings();
                            var hiringToRemove = JsonConvert.DeserializeObject<List<Hiring>>(hirings).FirstOrDefault(h => h.Id.ToString() == id);
                            var candidatsList = await dbService.GetCandidats();
                            var candidatToRemove = candidatsList.FirstOrDefault(c => c.Id == candidatId);
                            var firstDepartment = (await dbService.GetDictionary()).First(dict => dict.Type == "Department");
                            var hrPosition = (await dbService.GetDictionary()).Where(dict => dict.Type == "Roles").First(role => role.Name == "HR");
                            var newEmployee = new Employee
                            {
                                FirstName = candidatToRemove.FirstName,
                                LastName = candidatToRemove.LastName,
                                PhoneNumber = candidatToRemove.PhoneNumber,
                                Email = candidatToRemove.Email,
                                StartDate = DateTime.Now,
                                Department = firstDepartment.Id.ToString(),
                                Hiring = hiringToRemove.Id.ToString(),
                                Position = hrPosition.Id.ToString()
                            };
                            await dbService.AddEmployeeAsync(newEmployee);
                        }
                        var description = form["Description"];
                        var dictionaryList = await dbService.GetDictionary();
                        var dictionary = dictionaryList.First(value => value.Name == status);
                        var hiringList = JsonConvert.DeserializeObject<List<Hiring>>(await dbService.GetHirings());
                        var hiring = hiringList.First(h => h.Id.ToString() == id);
                        hiring.Status = dictionary.Id.ToString();
                        hiring.Comm = description;
                        hiring.StatusDate = DateTime.Now;
                        await dbService.AddOrUpdateHiringAsync(hiring);
                        return Ok();
                    case "EditEmployeeMenu":
                        var empId = form["ID"];
                        var employees = await dbService.GetRawEmployees();
                        var employee = employees.FirstOrDefault(e => e.Id.ToString() == empId);
                        var departments = (await dbService.GetDictionary()).Where(dict => dict.Type == "Department").ToList();
                        var depNames = departments.Select(dep => dep.Name).ToList();
                        var empObj = new
                        {
                            employee.FirstName,
                            employee.LastName,
                            employee.PhoneNumber,
                            employee.Email,
                            employee.Position,
                            Department = depNames
                        };
                        _json = JsonConvert.SerializeObject(empObj);
                        return Content(_json);
                    case "EditEmployee":
                        var editEmpId = form["ID"];
                        var editEmployees = JsonConvert.DeserializeObject<List<Employee>>(await dbService.GetEmployees());
                        var editEmployee = editEmployees.FirstOrDefault(e => e.Id.ToString() == editEmpId);
                        var editDepartments = (await dbService.GetDictionary()).Where(dict => dict.Type == "Department").ToList();
                        var selectedDepartment = editDepartments.First(dep => dep.Name == form["EmployeeDepartment"]);
                        editEmployee.FirstName = form["EmployeeFirstName"];
                        editEmployee.LastName = form["EmployeeLastName"];
                        editEmployee.Email = form["EmployeeEmail"];
                        editEmployee.Department = selectedDepartment.Id.ToString();
                        editEmployee.PhoneNumber = form["EmployeePhone"];
                        await dbService.UpdateEmployeeAsync(editEmployee);
                        return Ok();
                    case "EditDictionaryMenu":
                        var dictId = form["ID"];
                        var dictList = await dbService.GetDictionary();
                        var dictionaryMenu = dictList.First(d => d.Id.ToString() == dictId);
                        _json = JsonConvert.SerializeObject(dictionaryMenu);
                        return Content(_json);
                    case "EditDictionary":
                        var editDictId = form["ID"];
                        var editDictList = await dbService.GetDictionary();
                        var editDictionary = editDictList.First(d => d.Id.ToString() == editDictId);
                        editDictionary.Name = form["Name"];
                        editDictionary.Type = form["Type"];
                        editDictionary.Description = form["Description"];
                        await dbService.UpdateDictionaryAsync(editDictionary);
                        return Ok();
                    case "EditUser":
                        var editUserId = form["ID"];
                        var usersList = JsonConvert.DeserializeObject<List<User>>(await dbService.GetUsers());
                        var editUser = usersList.FirstOrDefault(u => u.Id.ToString() == editUserId);
                        editUser.FirstName = form["UserFirstName"];
                        editUser.LastName = form["UserLastName"];
                        editUser.UserName = form["UserUsername"];
                        editUser.Password = form["UserPassword"];
                        editUser.Email = form["UserEmail"];
                        await dbService.UpdateUserAsync(editUser);
                        return Ok();
                    case "EditUserMenu":
                        var userMenuId = form["ID"];
                        var userMenuList = await dbService.GetRawUsers();
                        var userMenu = userMenuList.FirstOrDefault(u => u.Id.ToString() == userMenuId);
                        _json = JsonConvert.SerializeObject(userMenu);
                        return Content(_json);
                    case "EditVacancyMenu":
                        var editVacancyMenuId = form["ID"];
                        var editVacancyMenuList = await dbService.GetVacancies();
                        var editVacancyMenu = editVacancyMenuList.FirstOrDefault(v => v.Id.ToString() == editVacancyMenuId);
                        var vacObj = new
                        {
                            VacancyID = editVacancyMenuId,
                            VacancyTitle = editVacancyMenu.Title,
                            VacancyDescription = editVacancyMenu.Description,
                            VacancyEndDate = editVacancyMenu.EndDate
                        };
                        _json = JsonConvert.SerializeObject(vacObj);
                        return Content(_json);
                    case "EditHiringMenu":
                        var editHiringMenuId = form["ID"];
                        var editHiringMenuList = JsonConvert.DeserializeObject<List<Hiring>>(await dbService.GetHirings());
                        var editHiringMenu = editHiringMenuList.FirstOrDefault(h => h.Id.ToString() == editHiringMenuId);
                        var statusName = (await dbService.GetDictionary()).First(dict => dict.Id == new ObjectId(editHiringMenu.Status));
                        var hirObj = new
                        {
                            HiringID = editHiringMenuId,
                            HiringCandidat = editHiringMenu.Candidat,
                            HiringUsers = editHiringMenu.Employee,
                            HiringStatus = statusName.Name,
                            HiringVacancy = editHiringMenu.Vacancy,
                            HiringStatusDate = editHiringMenu.StatusDate,
                            HiringComm = editHiringMenu.Comm
                        };
                        _json = JsonConvert.SerializeObject(hirObj);
                        return Content(_json);
                    case "DeleteVacancy":
                        var delVacancyId = form["ID"];
                        await dbService.DeleteVacancyAsync(delVacancyId);
                        return Content("Success");
                    case "GetCandidat":
                        var getCandidatId = form["ID"];
                        var getCandidatsList = await dbService.GetCandidats();
                        var getCandidat = getCandidatsList.FirstOrDefault(c => c.Id == getCandidatId);
                        return Content(JsonConvert.SerializeObject(getCandidat));
                    case "AssignRecruiter":
                        // Expecting CandidateID and RecruiterID from the frontend POST
                        var candidateId = form["CandidateID"];
                        var recruiterId = form["RecruiterID"];

                        // Get all hirings and try to find an existing hiring for this candidate
                        var allHirings = await dbService.GetRawHirings();

                        var candList = await dbService.GetCandidats();
                        var candidat = candList.First(candidat => candidat.Id == new ObjectId(candidateId));
                        var hiringForCandidate = allHirings.FirstOrDefault(h => h.Candidat == candidateId);

                        // Validate recruiter exists
                        var employeesList = await dbService.GetEmployees();
                        var allUsers = JsonConvert.DeserializeObject<List<EmployeeDto>>(employeesList);
                        var recruiterUser = allUsers.FirstOrDefault(u => u.Id.ToString() == recruiterId);
                        if (recruiterUser == null)
                        {
                            return NotFound("Recruiter not found");
                        }

                        if (hiringForCandidate == null)
                        {
                            // Create a new Hiring for this candidate and assign recruiter
                            var newHiring = new Hiring
                            {
                                Id = ObjectId.GenerateNewId(),
                                Candidat = candidateId,
                                Employee = recruiterId,
                                Status = "New", // Set default or required status if needed
                                Vacancy = candidat.VacancyName, // Set default or required vacancy if needed
                                StatusDate = DateTime.Now,
                                Comm = ""
                            };
                            await dbService.AddOrUpdateHiringAsync(newHiring); // Assuming upsert
                            await dbService.UpdateCandidatRecruiter(candidat.Id, recruiterId);
                            return Ok("New hiring created and recruiter assigned.");
                        }

                            // Update recruiter for existing hiring
                            hiringForCandidate.Employee = recruiterId;
                        await dbService.AddOrUpdateHiringAsync(hiringForCandidate);
                        return Ok("Recruiter assigned to existing hiring.");
                    case "DeleteUser":
                        var delUserId = form["ID"];
                        await dbService.DeleteUserAsync(delUserId);
                        return Ok();
                    case "DeleteEmployee":
                        var delEmployeeId = form["ID"];
                        await dbService.DeleteEmployeeAsync(delEmployeeId);
                        return Ok();
                    case "DeleteDictionary":
                        var delDictId = form["ID"];
                        await dbService.DeleteDictionaryAsync(delDictId);
                        return Ok();
                    case "DeleteCandidat":
                        var delCandidatId = form["ID"];
                        await dbService.DeleteCandidatAsync(delCandidatId);
                        return Ok();
                    case "GetCV":
                        // Implement file return logic if needed
                        return Ok();
                    default:
                        return Content(string.Empty);
                }
            }
            catch (Exception ex)
            {
                logger.LogError(ex.Message + " " + ex.StackTrace);
                return StatusCode(500, "Internal server error");
            }
        }
    }
}
