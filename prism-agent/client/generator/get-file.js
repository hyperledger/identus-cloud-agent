import { get } from 'http'; // or 'https' for https:// URLs
import { createWriteStream } from 'fs';

const PORT = process.env.PRISM_AGENT_PORT || 8080
const file = createWriteStream("oas.yml");
get(`http://localhost:${PORT}/docs/docs.yaml`, function(response) {
   response.pipe(file);

   file.on("finish", () => {
       file.close();
   });
});
