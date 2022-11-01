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
| `fint.fiks.svarut.service-url`             | `https://test.svarut.ks.no/tjenester/forsendelseservice/ForsendelsesServiceV11` |              |
| `fint.fiks.svarut.username`                |                                                                                 |              |
| `fint.fiks.svarut.password`                |                                                                                 |              |
| `fint.fiks.svarut.connection-timeout`      | `120000`                                                                        |              |
| `fint.fiks.svarut.receive-timeout`         | `120000`                                                                        |              |
| `fint.fiks.svarut.organisation.number`     |                                                                                 |              |
| `fint.fiks.svarut.organisation.name`       |                                                                                 |              |
| `fint.fiks.svarut.organisation.adresse1`   |                                                                                 |              |
| `fint.fiks.svarut.organisation.adresse2`   | ''                                                                              |              |
| `fint.fiks.svarut.organisation.adresse3`   | ''                                                                              |              |
| `fint.fiks.svarut.organisation.postalcode` |                                                                                 |              |
| `fint.fiks.svarut.organisation.city`       |                                                                                 |              |
| `fint.fiks.svarut.leveringsmetode`         | `KUN_DIGITAL_UTEN_LEVERANSEGARANTI_MASSEUTSENDELSE`                             |              |


## More information on configuration
- **[SSE Configuration](https://github.com/FINTLabs/fint-sse#sse-configuration)**
- **[OAuth Configuration](https://github.com/FINTLabs/fint-sse#oauth-configuration)** 
