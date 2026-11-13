# FINT _faks_ Adapter
This adapter integrates with KS FIKS SvarUT.

## Adapter configuration
| Key                                        | Description                                                                     | Default      |
|--------------------------------------------|---------------------------------------------------------------------------------|--------------|
| `fint.adapter.organizations`               | List of orgIds the adapter handles.                                             |              |
| `fint.adapter.endpoints.sse`               | Url to the sse endpoint for provider                                            | `/sse/%s`    |
| `fint.adapter.endpoints.status`            | Url to the status endpoint for provider                                         | `/status`    |
| `fint.adapter.endpoints.response`          | Url to the response endpoint for provider                                       | `/response`  |
| `fint.adapter.endpoints.providers.*`       | Baseurl for the `*` provider (see below)                                        |              |
| `fint.adapter.sse-expiration`              | Expiration for SSE messages                                                     | `1200000`    |                                                        
| `fint.internal-files.directory`            | Location for `FILE` based internal files                                        | `file-cache` |                                                        
| `fint.internal-files.connection-string`    | Azure connection string to storage account for `BLOB` based internal files      |              |
| `fint.internal-files.type`                 | Location for internal files, `BLOB` or `FILE`.                                  |              |
| `fint.fiks.svarut.service-url`             | `https://test.svarut.ks.no/api` |              |
| `fint.fiks.svarut.connection-timeout`      | `120000`                                                                        |              |
| `fint.fiks.svarut.receive-timeout`         | `120000`                                                                        |              |
| `fint.fiks.svarut.organization.number`     |                                                                                 |              |
| `fint.fiks.svarut.organization.name`       |                                                                                 |              |
| `fint.fiks.svarut.organization.address1`   |                                                                                 |              |
| `fint.fiks.svarut.organization.address2`   | ''                                                                              |              |
| `fint.fiks.svarut.organization.address3`   | ''                                                                              |              |
| `fint.fiks.svarut.organization.postal-code` |                                                                                 |              |
| `fint.fiks.svarut.organization.city`       |                                                                                 |              |


## More information on configuration
- **[SSE Configuration](https://github.com/FINTLabs/fint-sse#sse-configuration)**
- **[OAuth Configuration](https://github.com/FINTLabs/fint-sse#oauth-configuration)** 
