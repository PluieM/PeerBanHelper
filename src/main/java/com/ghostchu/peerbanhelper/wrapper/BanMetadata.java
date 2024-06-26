package com.ghostchu.peerbanhelper.wrapper;

import com.ghostchu.peerbanhelper.peer.Peer;
import com.ghostchu.peerbanhelper.torrent.Torrent;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class BanMetadata extends PeerMetadata implements Comparable<PeerMetadata> {
    private String context;
    private long banAt;
    private long unbanAt;
    private String rule;
    private String description;

    public BanMetadata(String context, String downloader, long banAt, long unbanAt, Torrent torrent, Peer peer, String rule,
                       String description) {
        super(downloader, torrent, peer);
        this.context = context;
        this.banAt = banAt;
        this.unbanAt = unbanAt;
        this.rule = rule;
        this.description = description;
    }

    public BanMetadata(String context, String downloader, long banAt, long unbanAt, TorrentWrapper torrent, PeerWrapper peer, String rule,
                       String description) {
        super(downloader, torrent, peer);
        this.context = context;
        this.banAt = banAt;
        this.unbanAt = unbanAt;
        this.rule = rule;
        this.description = description;
    }
}
