from vits import commons
import sys
import torch
from vits import utils
from vits.models import SynthesizerTrn
from vits.text import cleaned_text_to_sequence
from vits.text.cleaners import japanese_cleaners
from vits.text.symbols import symbols
from scipy.io.wavfile import write
import tempfile

hps = utils.get_hparams_from_file("./vits/config.json")
net_g = SynthesizerTrn(
    len(symbols),
    hps.data.filter_length // 2 + 1,
    hps.train.segment_size // hps.data.hop_length,
    **hps.model)
_ = net_g.eval()
_ = utils.load_checkpoint("./vits/G_88000.pth", net_g, None)


def get_text(text, hps):
    text_norm = cleaned_text_to_sequence(text)
    if hps.data.add_blank:
        text_norm = commons.intersperse(text_norm, 0)
    text_norm = torch.LongTensor(text_norm)
    return text_norm


def jtts(text, save_path):
    stn_tst = get_text(japanese_cleaners(text), hps)
    with torch.no_grad():
        x_tst = stn_tst.unsqueeze(0)
        x_tst_lengths = torch.LongTensor([stn_tst.size(0)])
        audio = net_g.infer(x_tst, x_tst_lengths, noise_scale=.667, noise_scale_w=0.8, length_scale=1)[0][
            0, 0].data.float().numpy()
        write(save_path, hps.data.sampling_rate, audio)


def doTTS(text):
    with tempfile.NamedTemporaryFile(delete=False) as f:
        jtts(text, f.name)
        return f.name


if __name__ == '__main__':
    if len(sys.argv) != 4:
        exit(1)
    model_path = sys.argv[1]
    text = sys.argv[2]
    save_path = sys.argv[3]
    _ = utils.load_checkpoint(model_path, net_g, None)
    jtts(text, save_path)
