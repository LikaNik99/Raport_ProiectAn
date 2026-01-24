FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src
COPY ["HrWebRecruitment.csproj", "."]
RUN dotnet restore "./HrWebRecruitment.csproj"
COPY . .
WORKDIR "/src/."
RUN dotnet build "HrWebRecruitment.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "HrWebRecruitment.csproj" -c Release -o /app/publish /p:UseAppHost=false

FROM mcr.microsoft.com/dotnet/aspnet:9.0
WORKDIR /app
COPY --from=publish /app/publish .

EXPOSE 8080

ENTRYPOINT ["dotnet", "HrWebRecruitment.dll"]
