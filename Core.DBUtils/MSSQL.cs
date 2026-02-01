using System.Data;
using System.Data.SqlClient;
using System.Text;
using System.Xml;

namespace Core.DBUtils.Db
{
    public class Mssql : IDisposable
    {
        static readonly int DefaultCommandTimeout = new SqlCommand().CommandTimeout;
        readonly string ConnectionString;

        public Mssql(string connectionString)
        {
            if (String.IsNullOrEmpty(connectionString))
                throw new ApplicationException("ConnectionString property is empty");
            this.ConnectionString = connectionString;
        }

        public int CommandTimeout = DefaultCommandTimeout;
        SqlConnection? conn;
        SqlTransaction? tran;
        enum CommandAction { Execute, Open, GetXml, Scalar }

        public void Open()
        {
            if (conn == null)
                conn = new SqlConnection();

            if (conn.State == ConnectionState.Broken)
                conn.Close();

            if (conn.State != ConnectionState.Closed) return;

            conn.ConnectionString = this.ConnectionString;
            conn.Open();
        }

        public async Task OpenAsync(CancellationToken cancellationToken)
        {
            if (conn == null)
                conn = new SqlConnection();

            if (conn.State == ConnectionState.Broken)
                conn.Close();

            if (conn.State != ConnectionState.Closed) return;

            conn.ConnectionString = this.ConnectionString;
            await conn.OpenAsync(cancellationToken);
        }

        public void Dispose() { Close(); }

        public void Close()
        {
            tran = null;
            conn?.Close();
            conn = null;
        }

        SqlCommand PrepareCommand(string cmdText, CommandType cmdType, SqlParameter[]? cmdParams)
        {
            if ((cmdType != CommandType.StoredProcedure) && (cmdType != CommandType.Text))
                throw new ArgumentOutOfRangeException("cmdType");

            SqlCommand cmd = new SqlCommand(cmdText, conn, tran);
            cmd.CommandType = cmdType;
            cmd.CommandTimeout = CommandTimeout;

            if (cmdParams != null)
                foreach (SqlParameter Param in cmdParams)
                    if (Param != null)
                    {
                        if (Param.Value == null)
                        {
                            Param.Value = DBNull.Value;
                            if ((Param.Direction == ParameterDirection.Output) && (Param.Size == 0))
                                switch (Param.DbType)
                                {
                                    case DbType.AnsiString:
                                    case DbType.AnsiStringFixedLength:
                                    case DbType.Binary:
                                    case DbType.String:
                                    case DbType.StringFixedLength:
                                    case DbType.Xml:
                                        Param.Size = -1;
                                        break;
                                }
                        }
                        cmd.Parameters.Add(Param);
                    }
            return cmd;
        }

        public DataSet SQLTextOpen(string SQLText, SqlParameter[] Params) 
            { return (DataSet)DoCommand(SQLText, CommandType.Text, CommandAction.Open, Params)!; }
        public async Task<DataSet> SQLTextOpenAsync(string SQLText, SqlParameter[] Params, CancellationToken cancellationToken) 
            { return (DataSet)(await DoCommandAsync(SQLText, CommandType.Text, CommandAction.Open, Params, cancellationToken))!; }
        public object SQLScalar(string SQLText, SqlParameter[] Params)
        {
            return DoCommand(SQLText, CommandType.Text, CommandAction.Scalar, Params)!;
        }

        public Task<object> SQLScalarAsync(string SQLText, SqlParameter[] Params, CancellationToken cancellationToken)
        {
            return DoCommandAsync(SQLText, CommandType.Text, CommandAction.Scalar, Params, cancellationToken)!;
        }

        public DataSet StoredProcOpen(string StoredProcName, SqlParameter[] StoredProcParams)
        { return (DataSet)DoCommand(StoredProcName, CommandType.StoredProcedure, CommandAction.Open, StoredProcParams)!; }

        public async Task<DataSet> StoredProcOpenAsync(string storedProcName, SqlParameter[] storedProcParams, CancellationToken cancellationToken)
        { return (DataSet)(await DoCommandAsync(storedProcName, CommandType.StoredProcedure, CommandAction.Open, storedProcParams, cancellationToken))!; }
        private object? DoCommand(string cmdText, CommandType cmdType, CommandAction cmdAction, SqlParameter[]? cmdParams)
        {
            SqlCommand cmd = PrepareCommand(cmdText, cmdType, cmdParams);
            try
            {
                switch (cmdAction)
                {
                    case CommandAction.Scalar:
                        return cmd.ExecuteScalar();
                    case CommandAction.Execute:
                        cmd.ExecuteNonQuery();
                        return null;
                    case CommandAction.Open:
                        DataSet ds = new DataSet();
                        SqlDataAdapter da = new SqlDataAdapter(cmd);
                        da.Fill(ds);
                        return ds;
                    case CommandAction.GetXml:
                        using (XmlReader xr = cmd.ExecuteXmlReader())
                        {
                            xr.MoveToContent();
                            StringBuilder sb = new StringBuilder();
                            while (!xr.EOF)
                                sb.Append(xr.ReadOuterXml());
                            return sb.ToString();
                        }
                    default: throw new ArgumentOutOfRangeException("cmdAction");
                }
            }
            catch (Exception e)
            {
                if (e is SqlException)
                    throw (SqlException)e;
                else
                {
                    Close();
                    throw;
                }
            }
        }

        private async Task<object?> DoCommandAsync(string cmdText, CommandType cmdType, CommandAction cmdAction, SqlParameter[]? cmdParams, CancellationToken cancellationToken)
        {
            SqlCommand cmd = PrepareCommand(cmdText, cmdType, cmdParams);
            try
            {
                switch (cmdAction)
                {
                    case CommandAction.Scalar:
                        return await cmd.ExecuteScalarAsync(cancellationToken);

                    case CommandAction.Execute:
                        await cmd.ExecuteNonQueryAsync(cancellationToken);
                        return null;

                    case CommandAction.Open:
                        var ds = new DataSet();
                        using (var reader = await cmd.ExecuteReaderAsync(cancellationToken))
                            await this.FillDataSetAsync(ds, reader, CancellationToken.None);
                        return ds;

                    case CommandAction.GetXml:
                        using (XmlReader xr = await cmd.ExecuteXmlReaderAsync(cancellationToken))
                        {
                            await xr.MoveToContentAsync();
                            var sb = new StringBuilder();
                            while (!xr.EOF)
                            {
                                var xml = await xr.ReadOuterXmlAsync();
                                sb.Append(xml);
                            }
                            return sb.ToString();
                        }

                    default: throw new ArgumentOutOfRangeException("cmdAction");
                }
            }
            catch (Exception e)
            {
                if (e is SqlException)
                    throw (SqlException)e;
                else
                {
                    Close();
                    throw;
                }
            }
        }

        private async Task FillDataSetAsync(DataSet ds, SqlDataReader openedReader, CancellationToken cancellationToken)
        {
            do
            {
                var dt = new DataTable();
                var read = await openedReader.ReadAsync(cancellationToken);
                for (int columnIndex = 0; columnIndex < openedReader.FieldCount; columnIndex++)
                {
                    var columnName = openedReader.GetName(columnIndex);
                    var columnType = openedReader.GetFieldType(columnIndex);
                    dt.Columns.Add(columnName, columnType);
                }

                ds.Tables.Add(dt);

                if (!read)
                    continue;

                do
                {
                    var row = dt.NewRow();
                    for (int columnIndex = 0; columnIndex < openedReader.FieldCount; columnIndex++)
                        row[columnIndex] = openedReader[columnIndex];
                    dt.Rows.Add(row);
                }
                while (await openedReader.ReadAsync(cancellationToken));
            }
            while (await openedReader.NextResultAsync(cancellationToken));
        }
    }
}
