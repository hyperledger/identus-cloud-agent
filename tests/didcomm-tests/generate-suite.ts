import * as fs from 'fs'
import * as path from 'path'
import * as crypto from 'crypto'
import { execSync } from 'child_process';

function hashString(stringToHash: string) {
    const hash = crypto.createHash('sha256')
    hash.update(stringToHash);
    const hashDigest = hash.digest('hex')
    return hashDigest;
}

function generate(dirPath: string, output: string) {
    // delete
    if (fs.existsSync(output)) {
        fs.rmSync(output)
    }

    const dir = path.dirname(output);
    fs.mkdirSync(dir, { recursive: true });

    // generate
    fs.readdirSync(dirPath).forEach(file => {
        const stat = fs.statSync(`${dirPath}/${file}`)
        if (stat.isFile()) {
            fs.appendFileSync(output, `export * as _${hashString(file)} from '${dirPath}/${file.replace('.ts', '')}'\n`)
        }
    })
}

function preGenerationFix() {
    const kyUniversal = 'node_modules/@hyperledger/identus-edge-agent-sdk/node_modules/ky-universal/index.js'
    const fileContent = fs.readFileSync(kyUniversal, 'utf8')
    let newContent = fileContent
        .replace(
            "await import('web-streams-polyfill/ponyfill/es2018');",
            "import('web-streams-polyfill/ponyfill/es2018');"
        ).replace(
            "await import('ky');",
            "import('ky');"
        )
    fs.writeFileSync(kyUniversal, newContent, 'utf8')
}

function postGenerationFix() {
    const filePath = './dist/suite.spec.js'
    const fileContent = fs.readFileSync(filePath, 'utf8')
    const newContent = fileContent.replace(
        "getObject(arg0).require(getStringFromWasm0(arg1, arg2));",
        "require(getStringFromWasm0(arg1, arg2));"
    )
    fs.writeFileSync(filePath, newContent, 'utf8')
}

// generates a temporary `test-suite` file that's used by esbuild
// to generate the bundled file.
(function () {
    generate('./test', 'test-suite.ts')
    preGenerationFix()
    execSync('node esbuild.config.js')
    execSync('rm test-suite.ts')
    postGenerationFix()
})()
