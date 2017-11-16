<?php
/**
 * Helpers
 */
function toCsv(array $data, $delimiter = ",", $eol = "\n", \Closure $callback = null)
{
    $callback = $callback ?: function (array $values) use ($delimiter) {
        return join($delimiter, $values);
    };

    return join($eol, array_map($callback, $data));
}

function toTsv(array $data, \Closure $callback = null)
{
    return toCsv($data, "\t", "\n", $callback);
}

function toTable(array $data)
{
    return sprintf("%%table %s", toTsv($data));
}

function toJson(array $data)
{
    return json_encode($data);
}

function fromCsv($value)
{
}

function fromTable($value)
{
}

function fromJson($value)
{
    return json_decode($value, true);
}
