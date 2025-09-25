<?php
$qs     = $_SERVER['QUERY_STRING'] ?? '';
$url    = 'http://helios.cs.ifmo.ru:8190/fcgi-bin/hello-world.jar' . ($qs ? '?' . $qs : '');
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

$body    = file_get_contents('php://input');
$hasBody = ($body !== '' && $body !== false);

$reqHeaders = "Accept: */*\r\n";
if ($method === 'POST') {
    $reqHeaders .= $hasBody
        ? "Content-Type: application/x-www-form-urlencoded\r\n"
        : "Content-Length: 0\r\n";
}

$ctx = stream_context_create([
    'http' => [
        'method'        => $method,
        'timeout'       => 4,
        'ignore_errors' => true,
        'header'        => $reqHeaders,
        'content'       => $hasBody ? $body : '',
    ],
]);

$fp = @fopen($url, 'rb', false, $ctx);
if (!$fp) {
    http_response_code(502);
    header('Content-Type: text/plain; charset=UTF-8');
    echo 'Bad Gateway';
    exit;
}

$codeSet = false;
$headers = stream_get_meta_data($fp)['wrapper_data'] ?? [];
foreach ($headers as $h) {
    if (preg_match('#^HTTP/\d(?:\.\d)?\s+(\d{3})#', $h, $m)) {
        http_response_code((int)$m[1]);
        $codeSet = true;
        continue;
    }
    $p = strpos($h, ':');
    if ($p !== false && trim(substr($h, 0, $p)) !== '') {
        header($h, false);
    }
}

if (!$codeSet) {
    http_response_code(200);
}

fpassthru($fp);
fclose($fp);
