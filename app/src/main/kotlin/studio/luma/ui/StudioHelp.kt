package studio.luma.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studio.luma.DraftState
import studio.luma.StudioUi

internal fun draftLabel(state:StudioUi)=if(!state.dirty) "Projeto salvo" else when(state.draftState) {
    DraftState.WAITING,DraftState.SAVING->"Salvando rascunho…"
    DraftState.SAVED->"Rascunho salvo"
    DraftState.FAILED->"Rascunho não salvo"
}

@Composable internal fun StudioHelpDialog(dismiss:()->Unit) {
    AlertDialog(onDismissRequest=dismiss,title={Text("Como usar o Luma")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(18.dp)) {
            HelpStep("1. Escolha sua foto","Toque em Editar foto. Seu arquivo original fica preservado.")
            HelpStep("2. Experimente um filtro","Toque numa miniatura para ver na sua foto. Ajuste a intensidade ou escolha Original para remover o filtro.")
            HelpStep("3. Ajuste o que precisar","Luz e cor clareia ou muda as cores. Recortar muda o enquadramento. Desfazer volta um passo.")
            HelpStep("4. Salve na galeria","Toque em Salvar foto. A opção Original mantém o tamanho da foto editada; 4K amplia o tamanho, sem recuperar detalhes por si só.")
            HelpStep("Continue depois","O rascunho é salvo automaticamente. Espere aparecer Rascunho salvo. Em Projetos, toque na foto para continuar.")
            HelpStep("Crie um Story","No início, toque em Criar Story, escolha um modelo, preencha as fotos e edite os textos.")
            HelpStep("Melhorar com IA","Essa opção usa seu PC conectado e ligado. Confira a comparação antes de aceitar a cópia melhorada.")
            Text("Mais ferramentas reúne máscaras, estilos e controles detalhados. Você pode voltar ao modo simples a qualquer momento.",fontSize=16.sp,lineHeight=23.sp)
        }
    },confirmButton={TextButton(onClick=dismiss) {Text("Entendi",fontSize=16.sp)}})
}

@Composable private fun HelpStep(title:String,body:String) {
    Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(title,fontSize=18.sp);Text(body,fontSize=16.sp,lineHeight=23.sp,color=Muted)}
}
