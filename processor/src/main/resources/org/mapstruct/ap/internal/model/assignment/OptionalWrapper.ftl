<#if sourceOptional && targetOptional>
${assignment}.map( arg -> ${conversion("arg")}  )
<#elseif sourceOptional>
    (${assignment} == null || !${assignment}.isPresent() ) ? ${defaultValue!"null"} : ${assignment}.map( arg -> ${conversion("arg")} ).get()
<#elseif targetOptional>
    Optional.of(${assignment}).map(arg -> ${conversion("arg")})
</#if>